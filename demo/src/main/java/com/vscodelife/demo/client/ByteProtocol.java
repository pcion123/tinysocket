package com.vscodelife.demo.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.demo.entity.ChatMessage;
import com.vscodelife.demo.entity.User;
import com.vscodelife.socketio.annotation.ProtocolTag;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

public class ByteProtocol {
    private static final Logger logger = LoggerFactory.getLogger(ByteProtocol.class);

    public static TestByteClient client;

    @ProtocolTag(mainNo = 0, subNo = 2, describe = "認證結果處理")
    public static void rcvAuthResult(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify auth result, code={}, msg={}", sessionId, requestId,
                code, msg);
        if (code == 200) {
            String token = message.getBuffer().readString();
            client.setAuth(true);
            client.setToken(token);
            logger.debug("sessionId={} requestId={} rcv server auth token={}", sessionId, requestId, token);
            //
            client.ping();
            //
            client.send(ProtocolId.ONLINE, new ByteArrayBuffer());
        }
    }

    @ProtocolTag(mainNo = 0, subNo = 126, describe = "會話ID通知")
    public static void rcvSessionId(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        long id = message.getBuffer().readLong();
        logger.debug("sessionId={} requestId={} rcv server notify sessionId={}", sessionId, requestId,
                id);
        if (client != null) {
            client.setSessionId(id);
        }
    }

    @ProtocolTag(mainNo = 1, subNo = 1, describe = "用戶上線通知")
    public static void rcvOnline(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify online, code={}, msg={}", sessionId, requestId,
                code, msg);
    }

    @ProtocolTag(mainNo = 1, subNo = 2, describe = "用戶離線通知")
    public static void rcvOffline(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify offline, code={}, msg={}", sessionId, requestId,
                code, msg);
    }

    @ProtocolTag(mainNo = 1, subNo = 3, describe = "獲取用戶列表響應")
    public static void rcvGetUserList(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify get user list, code={}, msg={}", sessionId, requestId,
                code, msg);
        if (code == 200) {
            List<User> users = message.getBuffer().readList(User.class);
            System.out.println("=== 在線用戶列表 ===");
            for (User user : users) {
                System.out.println(String.format("用戶編號: %s 用戶名稱: %s", user.getUserId(), user.getUserName()));
            }
        }
    }

    @ProtocolTag(mainNo = 1, subNo = 4, describe = "獲取用戶信息響應")
    public static void rcvGetUserInfo(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify get user info, code={}, msg={}", sessionId, requestId,
                code, msg);
        if (code == 200) {
            User user = message.getBuffer().readStruct(User.class);
            System.out.println("=== 用戶信息 ===");
            System.out.println(String.format("用戶編號: %s 用戶名稱: %s", user.getUserId(), user.getUserName()));
        }
    }

    @ProtocolTag(mainNo = 1, subNo = 5, describe = "發送消息響應")
    public static void rcvSay(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.debug("sessionId={} requestId={} rcv server notify say, code={}, msg={}", sessionId, requestId,
                code, msg);
    }

    @ProtocolTag(mainNo = 1, subNo = 6, describe = "接收聊天消息", safed = true)
    public static void rcvMessage(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        String userId = message.getHeader().getUserId();
        logger.debug("sessionId={} requestId={} rcv server notify message", sessionId, requestId);
        ChatMessage msg = message.getBuffer().readStruct(ChatMessage.class);
        if (msg.isSystemMessage()) {
            System.out.println(
                    String.format("[%s消息](%s) - %s", msg.getUserName(), msg.getTimestamp(), msg.getContent()));
        } else {
            if (userId.equals(msg.getUserId())) {
                System.out.println(
                        String.format("你(%s):\n%s", msg.getTimestamp(), msg.getContent()));
            } else {
                System.out.println(
                        String.format("%s(%s):\n%s", msg.getUserName(), msg.getTimestamp(), msg.getContent()));
            }
        }
    }
}
