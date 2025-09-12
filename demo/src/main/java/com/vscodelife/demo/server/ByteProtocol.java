package com.vscodelife.demo.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.demo.entity.ChatMessage;
import com.vscodelife.demo.entity.User;
import com.vscodelife.demo.server.component.ChatManager;
import com.vscodelife.demo.server.component.UserManager;
import com.vscodelife.socketio.annotation.ProtocolTag;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

public final class ByteProtocol {
    private static final Logger logger = LoggerFactory.getLogger(ByteProtocol.class);

    public static TestByteServer server;

    @ProtocolTag(mainNo = 0, subNo = 1, cached = false, safed = true, describe = "auth")
    public static void auth(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getBuffer().readString();
        String password = message.getBuffer().readString();
        logger.error("sessionId={} requestId={} rcv client ask auth request, userId={}, password={}", sessionId,
                requestId, userId, password);
    }

    @ProtocolTag(mainNo = 1, subNo = 1, cached = true, safed = true, describe = "online")
    public static void online(ByteMessage<ByteUserHeader> message) {
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
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("success");
        response.writeStruct(recentMessages);
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶上線
        ByteArrayBuffer broadcastMsg = new ByteArrayBuffer();
        broadcastMsg.writeStruct(msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }

    @ProtocolTag(mainNo = 1, subNo = 2, cached = true, safed = true, describe = "offline")
    public static void offline(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        logger.info("sessionId={} requestId={} rcv client offline request, content={}", sessionId, requestId);
        //
        ChatMessage msg = ChatManager.getInstance().userOfflineWithMessage(userId);
        // 回應用戶信息
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("success");
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶下線
        ByteArrayBuffer broadcastMsg = new ByteArrayBuffer();
        broadcastMsg.writeStruct(msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }

    @ProtocolTag(mainNo = 1, subNo = 3, cached = true, safed = true, describe = "get user list")
    public static void getUserList(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        logger.info("sessionId={} requestId={} rcv client get user list request", sessionId, requestId);
        //
        List<User> users = ChatManager.getInstance().getAllOnlineUsers();
        // 回應用戶信息
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("success");
        response.writeList(users);
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
    }

    @ProtocolTag(mainNo = 1, subNo = 4, cached = true, safed = true, describe = "get user info")
    public static void getUserInfo(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String targetId = message.getBuffer().readString();
        logger.info("sessionId={} requestId={} rcv client get user info request, targetId={}", sessionId, requestId,
                targetId);
        //
        User user = ChatManager.getInstance().getUser(targetId);
        if (user == null) {
            ByteArrayBuffer response = new ByteArrayBuffer();
            response.writeInt(404);
            response.writeString("user not found");
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        } else {
            ByteArrayBuffer response = new ByteArrayBuffer();
            response.writeInt(200);
            response.writeString("success");
            response.writeStruct(user);
            server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        }
    }

    @ProtocolTag(mainNo = 1, subNo = 5, cached = true, safed = true, describe = "say")
    public static void say(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        long requestId = message.getHeader().getRequestId();
        String userId = message.getHeader().getUserId();
        String content = message.getBuffer().readString();
        logger.info("sessionId={} requestId={} rcv client say request, content={}", sessionId, requestId, content);
        //
        ChatMessage msg = ChatManager.getInstance().addMessage(userId, content);
        // 回應用戶信息
        ByteArrayBuffer response = new ByteArrayBuffer();
        response.writeInt(200);
        response.writeString("success");
        server.send(sessionId, message.getHeader().getProtocolKey(), requestId, response);
        // 廣播用戶發話
        ByteArrayBuffer broadcastMsg = new ByteArrayBuffer();
        broadcastMsg.writeStruct(msg);
        server.broadcast(ProtocolId.MESSAGE, broadcastMsg);
    }

}
