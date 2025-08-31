package com.vscodelife.demo.server.handler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.ByteUserConnection;
import com.vscodelife.demo.server.ByteUserHeader;
import com.vscodelife.demo.server.TestByteServer;
import com.vscodelife.demo.server.exception.AuthException;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.ByteMessage;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.util.JwtUtil;

import io.jsonwebtoken.Claims;
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
                if (ProtocolId.AUTH != protocolKey) {
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
        String token = request.readString();
        // 驗證用戶
        if (!validateUser(userId, token)) {
            throw new AuthException("validate user failed");
        }
        // 標記為已驗證
        setAuthenticated(ctx, true);
        // 取消超時任務
        cancelAuthTimeout(ctx);
        // 從 pipeline 中移除自己，後續消息直接進入正常處理流程
        removeAuthHandler(ctx);
        // 發送驗證成功消息
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("auth success");
        connection.send(ProtocolId.AUTH_RESULT, requestId, response);
    }

    private boolean validateUser(String userId, String token) throws AuthException {
        try {
            // 驗證 JWT Token
            Claims claims = JwtUtil.parseJws(token);
            if (claims == null) {
                throw new AuthException("claims is null");
            }
            Object userIdClaim = claims.get("userId");
            if (userIdClaim == null) {
                throw new AuthException("userId claim is null");
            }
            if (!userId.equals(userIdClaim.toString())) {
                throw new AuthException("userId mismatch");
            }
            return true;
        } catch (RuntimeException e) {
            // JWT 解析或驗證失敗
            throw new AuthException("JWT validation failed: " + e.getMessage());
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
