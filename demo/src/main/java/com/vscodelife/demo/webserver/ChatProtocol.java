package com.vscodelife.demo.webserver;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.demo.entity.ChatMessage;
import com.vscodelife.demo.entity.User;
import com.vscodelife.demo.webserver.component.ChatManager;
import com.vscodelife.demo.webserver.component.UserManager;
import com.vscodelife.demo.webserver.util.TokenUtil;
import com.vscodelife.socketio.annotation.ProtocolTag;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.JsonMessage;

/**
 * 聊天服務器的協議處理器
 * 處理各種聊天相關的協議
 */
public final class ChatProtocol {
    private static final Logger logger = LoggerFactory.getLogger(ChatProtocol.class);

    public static ChatWebServer server;

    @ProtocolTag(mainNo = 0, subNo = 1, cached = false, safed = true, describe = "auth")
    public static void login(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getBuffer().getString("userId");
        String password = message.getBuffer().getString("password");
        logger.error("sessionId={} requestId={} rcv client ask auth request, userId={}, password={}", sessionId,
                requestId, userId, password);
    }

    @ProtocolTag(mainNo = 0, subNo = 125, cached = false, safed = true, describe = "reflash token")
    public static void reflash(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        String currentToken = message.getHeader().getToken();

        logger.info("sessionId={} requestId={} rcv client reflash token request for userId={}", sessionId, requestId,
                userId);

        try {
            // 驗證當前 token 是否有效
            if (currentToken == null || currentToken.trim().isEmpty()) {
                JsonMapBuffer response = new JsonMapBuffer();
                response.put("code", 401);
                response.put("message", "current token is required");
                server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
                return;
            }

            // 驗證 token 中的用戶ID是否與請求的用戶ID一致
            String tokenUserId = TokenUtil.extractUserId(currentToken);
            if (tokenUserId == null || !tokenUserId.equals(userId)) {
                JsonMapBuffer response = new JsonMapBuffer();
                response.put("code", 401);
                response.put("message", "token validation failed");
                server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
                return;
            }

            // // 檢查 token 是否即將過期（5分鐘內過期才允許刷新）
            // if (!TokenUtil.isTokenExpiringSoon(currentToken, 5)) {
            // JsonMapBuffer response = new JsonMapBuffer();
            // response.put("code", 400);
            // response.put("message", "token is not expiring soon, refresh not needed");
            // server.send(sessionId, message.getHeader().getProtocolKey(), requestId,
            // response);
            // return;
            // }

            // 產生新的 token（30分鐘有效期）
            String newToken = TokenUtil.generateAuthToken(userId, 30);

            // 回應用戶信息
            JsonMapBuffer response = new JsonMapBuffer();
            response.put("code", 200);
            response.put("message", "token refreshed successfully");
            response.put("token", newToken);
            response.put("expiresIn", 30 * 60); // 30分鐘，以秒為單位
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);

            logger.info("Token refreshed successfully for userId: {}, new token expires in 30 minutes", userId);

        } catch (Exception e) {
            logger.error("Failed to refresh token for userId: {}", userId, e);

            JsonMapBuffer response = new JsonMapBuffer();
            response.put("code", 500);
            response.put("message", "token refresh failed: " + e.getMessage());
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        }
    }

    @ProtocolTag(mainNo = 0, subNo = 126, cached = false, safed = true, describe = "ask sessionId")
    public static void askSessionId(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        logger.error("sessionId={} requestId={} rcv client ask session id request", sessionId, requestId);
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "success");
        response.put("sessionId", sessionId);
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
    }

    @ProtocolTag(mainNo = 1, subNo = 1, cached = true, safed = true, describe = "online")
    public static void online(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        logger.info("sessionId={} requestId={} rcv client online request", sessionId, requestId);
        //
        User user = UserManager.getInstance().getUser(userId);
        //
        ChatMessage msg = ChatManager.getInstance().userOnlineWithMessage(user);
        //
        List<ChatMessage> recentMessages = ChatManager.getInstance().getRecentMessages(10);
        // 回應用戶信息
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "success");
        response.put("recentMessages", recentMessages);
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶上線
        JsonMapBuffer broadcastMsg = new JsonMapBuffer();
        broadcastMsg.put("message", msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }

    @ProtocolTag(mainNo = 1, subNo = 2, cached = true, safed = true, describe = "offline")
    public static void offline(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        logger.info("sessionId={} requestId={} rcv client offline request, content={}", sessionId, requestId);
        //
        ChatMessage msg = ChatManager.getInstance().userOfflineWithMessage(userId);
        // 回應用戶信息
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "success");
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶下線
        JsonMapBuffer broadcastMsg = new JsonMapBuffer();
        broadcastMsg.put("message", msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }

    @ProtocolTag(mainNo = 1, subNo = 3, cached = true, safed = true, describe = "get user list")
    public static void getUserList(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        logger.info("sessionId={} requestId={} rcv client get user list request", sessionId, requestId);
        //
        List<User> users = ChatManager.getInstance().getAllOnlineUsers();
        // 回應用戶信息
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "success");
        response.put("users", users);
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
    }

    @ProtocolTag(mainNo = 1, subNo = 4, cached = true, safed = true, describe = "get user info")
    public static void getUserInfo(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String targetId = message.getBuffer().getString("targetId");
        logger.info("sessionId={} requestId={} rcv client get user info request, targetId={}", sessionId, requestId,
                targetId);
        //
        User user = ChatManager.getInstance().getUser(targetId);
        if (user == null) {
            JsonMapBuffer response = new JsonMapBuffer();
            response.put("code", 404);
            response.put("message", "user not found");
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        } else {
            JsonMapBuffer response = new JsonMapBuffer();
            response.put("code", 200);
            response.put("message", "success");
            response.put("user", user);
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        }
    }

    @ProtocolTag(mainNo = 1, subNo = 5, cached = true, safed = true, describe = "say")
    public static void say(JsonMessage<ChatUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        String content = message.getBuffer().getString("content");
        logger.info("sessionId={} requestId={} rcv client say request, content={}", sessionId, requestId, content);
        //
        ChatMessage msg = ChatManager.getInstance().addMessage(userId, content);
        // 回應用戶信息
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("code", 200);
        response.put("message", "success");
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶發話
        JsonMapBuffer broadcastMsg = new JsonMapBuffer();
        broadcastMsg.put("message", msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }
}
