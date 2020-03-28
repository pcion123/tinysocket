package com.tiny.socket.socketio.message;

import com.tiny.socket.socketio.buffer.ByteArrayBuffer;
import com.tiny.socket.socketio.header.HeaderBase;
import com.tiny.socket.socketio.message.base.MessageBase;
import io.netty.buffer.ByteBuf;

public class BinaryMessage extends MessageBase<ByteArrayBuffer> {
    public BinaryMessage() {
        super();
    }

    public BinaryMessage(HeaderBase header, ByteArrayBuffer buffer) {
        super(header, buffer);
    }

    public BinaryMessage(HeaderBase header, ByteBuf buffer) {
        super(header, new ByteArrayBuffer(buffer));
    }

    public BinaryMessage(HeaderBase header, byte[] buffer) {
        super(header, new ByteArrayBuffer(buffer));
    }

    @Override
    public boolean release() {
        return buffer != null ? buffer.release() : false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public MessageBase clone() {
        return new BinaryMessage(header.clone(), buffer.clone());
    }
}
