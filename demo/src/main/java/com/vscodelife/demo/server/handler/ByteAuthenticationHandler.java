package com.vscodelife.demo.server.handler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.entity.User;
import com.vscodelife.demo.server.ByteUserConnection;
import com.vscodelife.demo.server.ByteUserHeader;
import com.vscodelife.demo.server.TestByteServer;
import com.vscodelife.demo.server.component.UserManager;
import com.vscodelife.demo.server.exception.AuthException;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.ByteMessage;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.util.JwtUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * 用戶身份驗證 Handler
 * 處理客戶端首次連線時的身份驗證
 * 驗證成功後會從 pipeline 中移除自己
 */
public class ByteAuthenticationHandler extends SimpleChannelInboundHandler<ByteMessage<ByteUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ByteAuthenticationHandler.class);

    // 驗證超時時間（秒）
    private static final int AUTH_TIMEOUT_SECONDS = 30;

    // Channel 屬性鍵
    private static final AttributeKey<Boolean> AUTH_STATE_KEY = AttributeKey.valueOf("authenticated");
    private static final AttributeKey<ScheduledFuture<?>> AUTH_TIMEOUT_KEY = AttributeKey.valueOf("authTimeout");

    private final TestByteServer socket;
    private final ScheduledExecutorService scheduler;

    public ByteAuthenticationHandler(TestByteServer socket, ScheduledExecutorService scheduler) {
        this.socket = socket;
        this.scheduler = scheduler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client {} connected, waiting for authentication", ctx.channel().remoteAddress());

        // 設置初始驗證狀態為未驗證
        setAuthenticated(ctx, false);

        // 設置驗證超時
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            // 檢查是否驗證成功
            if (!isAuthenticated(ctx)) {
                logger.warn("Authentication timeout for client {}, closing connection",
                        ctx.channel().remoteAddress());
                // 取得用戶連接
                ByteUserConnection connection = socket.getConnection(ctx.channel());
                if (connection != null) {
                    ByteArrayBuffer response = new ByteArrayBuffer();
                    response.writeInt(408);
                    response.writeString("auth timeout");
                    connection.send(ProtocolId.AUTH_RESULT, response);
                    connection.disconnect();
                } else {
                    ctx.close();
                }
            }
        }, AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        ctx.channel().attr(AUTH_TIMEOUT_KEY).set(timeoutFuture);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 取消驗證超時任務
        cancelAuthTimeout(ctx);

        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> message) throws Exception {
        logger.info("Client {} send request, message={}", ctx.channel().remoteAddress(), message);
        // 取得消息頭
        ByteUserHeader header = message.getHeader();
        // 取得協議鍵
        ProtocolKey protocolKey = header.getProtocolKey();
        // 取得請求編號
        long requestId = header.getRequestId();

        // 取得用戶連接
        ByteUserConnection connection = socket.getConnection(ctx.channel());
        if (connection != null) {
            try {
                // 檢查是否為驗證協議
                if (!ProtocolId.AUTH.equals(protocolKey)) {
                    // 如果尚未通過驗證，則拒絕該消息處理
                    if (!isAuthenticated(ctx)) {
                        throw new AuthException("invalid protocol");
                    } else { // 已驗證，將消息pass給下個handler
                        ctx.fireChannelRead(message);
                    }
                } else {
                    handleAuthentication(ctx, connection, message);
                }
            } catch (AuthException e1) {
                ByteArrayBuffer response = new ByteArrayBuffer();
                response.writeInt(401);
                response.writeString(e1.getMessage());
                connection.send(ProtocolId.AUTH_RESULT, requestId, response);
                connection.disconnect();
            } catch (Exception e2) {
                ByteArrayBuffer response = new ByteArrayBuffer();
                response.writeInt(500);
                response.writeString(e2.getMessage());
                connection.send(ProtocolId.AUTH_RESULT, requestId, response);
                connection.disconnect();
                logger.error(String.format("process auth has unknown error=%s",
                        e2.getMessage()), e2);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in authentication handler for client {}: {}",
                ctx.channel().remoteAddress(), cause.getMessage(), cause);
        // 取消驗證超時任務
        cancelAuthTimeout(ctx);
        // 關閉連接
        ctx.close();
    }

    private void handleAuthentication(ChannelHandlerContext ctx, ByteUserConnection connection,
            ByteMessage<ByteUserHeader> message)
            throws AuthException {
        // 取得消息頭
        ByteUserHeader header = message.getHeader();
        // 取得請求編號
        long requestId = header.getRequestId();
        // 取得請求內容
        ByteArrayBuffer request = message.getBuffer();
        String userId = request.readString();
        String password = request.readString();

        // 驗證用戶帳號密碼
        if (!validateUserCredentials(userId, password)) {
            throw new AuthException("userId or password is error");
        }

        // 生成 JWT Token（15分鐘有效期）
        String token = generateAuthToken(userId);

        // 標記為已驗證
        setAuthenticated(ctx, true);
        // 取消超時任務
        cancelAuthTimeout(ctx);
        // 從 pipeline 中移除自己，後續消息直接進入正常處理流程
        removeAuthHandler(ctx);

        connection.setUserId(userId);
        connection.setToken(token);
        connection.setAuthed(true);

        // 發送驗證成功消息，包含生成的 token
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("auth success");
        response.writeString(token);
        connection.send(ProtocolId.AUTH_RESULT, requestId, response);

        logger.info("用戶 {} 驗證成功，生成 token: {}", userId, token);
    }

    /**
     * 驗證用戶帳號密碼
     */
    private boolean validateUserCredentials(String userId, String password) throws AuthException {
        User user = UserManager.getInstance().getUser(userId);
        if (user == null) {
            throw new AuthException("user is not exist");
        }
        return user.getPassword().equals(password);
    }

    /**
     * 生成驗證 token（15分鐘有效期）
     */
    private String generateAuthToken(String userId) {
        try {
            // 創建自定義聲明
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("authTime", Instant.now().getEpochSecond());
            claims.put("tokenType", "auth");

            // 使用自定義方法生成 15 分鐘過期的 token
            return generateJwsWithMinutes(userId, claims, 3);
        } catch (Exception e) {
            logger.error("create jwt token failed", e);
            // 不拋出 AuthException，而是拋出 RuntimeException
            throw new RuntimeException("jwt token create failed", e);
        }
    }

    /**
     * 生成指定分鐘數過期的 JWS Token
     */
    private String generateJwsWithMinutes(String subject, Map<String, Object> claims, int expirationMinutes) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationMinutes, java.time.temporal.ChronoUnit.MINUTES);

            io.jsonwebtoken.JwtBuilder builder = io.jsonwebtoken.Jwts.builder()
                    .subject(subject)
                    .issuedAt(java.util.Date.from(now))
                    .expiration(java.util.Date.from(expiration))
                    .signWith(JwtUtil.createKeyFromString("mySecretKeyForJWTTokenGenerationAndValidation12345"));

            if (claims != null && !claims.isEmpty()) {
                builder.claims(claims);
                builder.subject(subject); // 重新設置 subject，因為 claims 會覆蓋
            }

            return builder.compact();
        } catch (Exception e) {
            logger.error("create custom expiration JWS failed", e);
            throw new RuntimeException("create custom expiration JWS failed", e);
        }
    }

    private boolean isAuthenticated(ChannelHandlerContext ctx) {
        Boolean authenticated = ctx.channel().attr(AUTH_STATE_KEY).get();
        return authenticated != null && authenticated;
    }

    private void setAuthenticated(ChannelHandlerContext ctx, boolean authenticated) {
        ctx.channel().attr(AUTH_STATE_KEY).set(authenticated);
    }

    private void cancelAuthTimeout(ChannelHandlerContext ctx) {
        ScheduledFuture<?> timeoutFuture = ctx.channel().attr(AUTH_TIMEOUT_KEY).get();
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
    }

    private void removeAuthHandler(ChannelHandlerContext ctx) {
        logger.info("Removing authentication handler for client {}", ctx.channel().remoteAddress());
        ctx.pipeline().remove(this);
        logger.info("Authentication handler removed successfully for client {}", ctx.channel().remoteAddress());
    }

}
