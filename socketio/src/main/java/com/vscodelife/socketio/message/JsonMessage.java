package com.vscodelife.socketio.message;

import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

public class JsonMessage extends MessageBase<String> {
    public JsonMessage() {
        super();
    }

    public JsonMessage(HeaderBase header, String buffer) {
        super(header, buffer);
    }

    @Override
    public JsonMessage clone() {
        return new JsonMessage(header.clone(), buffer);
    }
}
