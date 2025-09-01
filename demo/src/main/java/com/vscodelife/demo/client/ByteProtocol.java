package com.vscodelife.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.message.ByteMessage;

public class ByteProtocol {
    private static final Logger logger = LoggerFactory.getLogger(ByteProtocol.class);

    public static TestByteClient client;

    public static void rcvAuthResult(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.info("sessionId={} requestId={} rcv server notify auth result, code={}, msg={}", sessionId, requestId,
                code, msg);
        if (code == 200) {
            String token = message.getBuffer().readString();
            client.setAuth(true);
            client.setToken(token);
            logger.info("sessionId={} requestId={} rcv server auth token={}", sessionId, requestId, token);
            client.ping();
        }

    }

    public static void rcvSessionId(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        long id = message.getBuffer().readLong();
        logger.info("sessionId={} requestId={} rcv server notify sessionId={}", sessionId, requestId,
                id);
        if (client != null) {
            client.setSessionId(id);
        }
    }
}
