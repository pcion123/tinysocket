package com.vscodelife.demo.server;

import com.vscodelife.socketio.message.ByteMessage;

public final class ByteProtocol {
    private TestByteServer socket;

    public ByteProtocol(TestByteServer socket) {
        this.socket = socket;
    }

    public static void auth(ByteMessage<ByteUserHeader> message) {

    }

}
