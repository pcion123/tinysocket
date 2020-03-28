package com.tiny.socket.socketio.message;

import com.tiny.socket.socketio.header.HeaderBase;
import com.tiny.socket.socketio.message.base.MessageBase;

public class WebMessage extends MessageBase<String> {
    public WebMessage() {
        super();
    }

    public WebMessage(HeaderBase header, String buffer) {
        super(header, buffer);
    }

    @Override
    public boolean release() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public MessageBase clone() {
        return new WebMessage(header.clone(), buffer);
    }
}
