package com.vscodelife.demo.server.exception;

public class ChatRuntimeException extends RuntimeException {
    public ChatRuntimeException(String message) {
        super(message);
    }

    public ChatRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
