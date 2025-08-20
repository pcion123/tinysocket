package com.vscodelife.socketio.message;

import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

public class ByteMessage extends MessageBase<ByteArrayBuffer> {
    public ByteMessage() {
        super();
    }

    public ByteMessage(HeaderBase header, ByteArrayBuffer buffer) {
        super(header, buffer);
    }

    @Override
    public ByteMessage clone() {
        return new ByteMessage(header.clone(), buffer.clone());
    }
}
