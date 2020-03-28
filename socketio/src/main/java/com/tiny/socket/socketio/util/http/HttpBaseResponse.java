package com.tiny.socket.socketio.util.http;

import com.tiny.socket.socketio.util.JsonUtil;

public class HttpBaseResponse {
    private int code;
    private String message;
    private String response;

    public HttpBaseResponse(int code, String message) {
        this(code, message, null);
    }

    public HttpBaseResponse(int code, String message, String response) {
        this.code = code;
        this.message = message;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("code=%d message=%s response=%s", code, message, response);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponse() {
        return response;
    }

    public <T> T getResponse(Class<T> clazz) {
        T result = null;
        if (response != null) {
            result = JsonUtil.toObject(response, clazz);
        }
        return checker(clazz, result);
    }

    public void setResponse(String response) {
        this.response = response;
    }

    private <T> T checker(Class<T> clazz, T result) {
        if (clazz != null) {
            if (clazz == Byte.class || clazz == Integer.class || clazz == Long.class) {
                return result != null ? result : clazz.cast(0);
            } else if (clazz == Float.class || clazz == Double.class) {
                return result != null ? result : clazz.cast(0f);
            } else if (clazz == Boolean.class) {
                return result != null ? result : clazz.cast(false);
            }
        }
        return result;
    }
}
