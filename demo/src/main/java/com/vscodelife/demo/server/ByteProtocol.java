package com.vscodelife.demo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.annotation.ProtocolTag;
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
        logger.info("sessionId={} requestId={} rcv client ask auth request, userId={}, password={}", sessionId,
                requestId,
                userId, password);
    }

}
