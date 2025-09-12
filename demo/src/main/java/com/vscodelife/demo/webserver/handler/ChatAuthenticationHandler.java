package com.vscodelife.demo.webserver.handler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.demo.entity.User;
import com.vscodelife.demo.exception.AuthException;
import com.vscodelife.demo.webserver.ChatUserConnection;
import com.vscodelife.demo.webserver.ChatUserHeader;
import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.demo.webserver.component.UserManager;
import com.vscodelife.demo.webserver.util.TokenUtil;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.JsonMessage;
import com.vscodelife.socketio.message.base.ProtocolKey;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * 用戶身份驗證 Handler
 * 處理客戶端首次連線時的身份驗證
 * 驗證成功後會從 pipeline 中移除自己
 */
public class ChatAuthenticationHandler extends SimpleChannelInboundHandler<JsonMessage<ChatUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ChatAuthenticationHandler.class);

    // 驗證超時時間（秒）
    private static final int AUTH_TIMEOUT_SECONDS = 30;

    // Channel 屬性鍵
    private static final AttributeKey<Boolean> AUTH_STATE_KEY = AttributeKey.valueOf("authenticated");
    private static final AttributeKey<ScheduledFuture<?>> AUTH_TIMEOUT_KEY = AttributeKey.valueOf("authTimeout");

    private final ChatWebServer socket;
    private final ScheduledExecutorService scheduler;

    public ChatAuthenticationHandler(ChatWebServer socket, ScheduledExecutorService scheduler) {
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
                ChatUserConnection connection = socket.getConnection(ctx.channel());
                if (connection != null) {
                    JsonMapBuffer response = new JsonMapBuffer();
                    response.put("code", 408);
                    response.put("message", "auth timeout");
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
    protected void channelRead0(ChannelHandlerContext ctx, JsonMessage<ChatUserHeader> message) throws Exception {
        logger.info("Client {} send request, message={}", ctx.channel().remoteAddress(), message);
        // 取得消息頭
        ChatUserHeader header = message.getHeader();
        // 取得協議鍵
        ProtocolKey protocolKey = header.getProtocolKey();
        // 取得請求編號
        long requestId = header.getRequestId();

        // 取得用戶連接
        ChatUserConnection connection = socket.getConnection(ctx.channel());
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
                JsonMapBuffer response = new JsonMapBuffer();
                response.put("code", 401);
                response.put("message", e1.getMessage());
                connection.send(ProtocolId.AUTH_RESULT, requestId, response);
                connection.disconnect();
            } catch (Exception e2) {
                JsonMapBuffer response = new JsonMapBuffer();
                response.put("code", 500);
                response.put("message", e2.getMessage());
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

    private void handleAuthentication(ChannelHandlerContext ctx, ChatUserConnection connection,
            JsonMessage<ChatUserHeader> message)
            throws AuthException {
        // 取得消息頭
        ChatUserHeader header = message.getHeader();
        // 取得請求編號
        long requestId = header.getRequestId();
        // 取得請求內容
        JsonMapBuffer request = message.getBuffer();
        String userId = request.getString("userId");
        String password = request.getString("password");

        // 驗證用戶帳號密碼
        if (!validateUserCredentials(userId, password)) {
            throw new AuthException("userId or password is error");
        }

        // 生成 JWT Token（30分鐘有效期）
        String token = TokenUtil.generateAuthToken(userId);

        // 標記為已驗證
        setAuthenticated(ctx, true);
        // 取消超時任務
        cancelAuthTimeout(ctx);
        // 從 pipeline 中移除自己，後續消息直接進入正常處理流程
        removeAuthHandler(ctx);

        connection.setUserId(userId);
        connection.setToken(token);
        connection.setAuthed(true);

        // 取得會話編號
        long sessionId = connection.getSessionId();

        // 發送驗證成功消息，包含生成的 token
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "auth success");
        response.put("sessionId", sessionId);
        response.put("token", token);
        connection.send(ProtocolId.AUTH_RESULT, requestId, response);

        logger.info("用戶 {} 驗證成功，生成 30分鐘有效期 token: {}", userId, token);
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
