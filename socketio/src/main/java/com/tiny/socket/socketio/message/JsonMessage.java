package com.tiny.socket.socketio.message;

import com.tiny.socket.socketio.header.HeaderBase;
import com.tiny.socket.socketio.message.base.MessageBase;

public class JsonMessage extends MessageBase<String> {
    public JsonMessage() {
        super();
    }

    public JsonMessage(HeaderBase header, String buffer) {
        super(header, buffer);
    }

    @Override
    public boolean release() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public MessageBase clone() {
        return new JsonMessage(header.clone(), buffer);
    }
}
