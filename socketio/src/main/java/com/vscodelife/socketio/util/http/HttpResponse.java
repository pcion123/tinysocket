package com.vscodelife.socketio.util.http;

import com.vscodelife.socketio.util.JsonUtil;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HttpResponse {
    private int code;
    private String message;
    private String response;

    // 便利建構子 - 不包含 response
    public HttpResponse(int code, String message) {
        this(code, message, null);
    }

    // 全參數建構子
    public HttpResponse(int code, String message, String response) {
        this.code = code;
        this.message = message;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("code=%d message=%s response=%s", code, message, response);
    }

    public <T> T getResponse(Class<T> clazz) {
        T result = null;
        if (response != null) {
            result = JsonUtil.fromJson(response, clazz);
        }
        return checker(result, clazz);
    }

    private <T> T checker(T result, Class<T> clazz) {
        if (clazz != null) {
            // 處理基本類型（primitive types）
            if (clazz == byte.class || clazz == Byte.class) {
                return result != null ? result : clazz.cast((byte) 0);
            } else if (clazz == short.class || clazz == Short.class) {
                return result != null ? result : clazz.cast((short) 0);
            } else if (clazz == int.class || clazz == Integer.class) {
                return result != null ? result : clazz.cast(0);
            } else if (clazz == long.class || clazz == Long.class) {
                return result != null ? result : clazz.cast(0L);
            } else if (clazz == float.class || clazz == Float.class) {
                return result != null ? result : clazz.cast(0.0f);
            } else if (clazz == double.class || clazz == Double.class) {
                return result != null ? result : clazz.cast(0.0d);
            } else if (clazz == boolean.class || clazz == Boolean.class) {
                return result != null ? result : clazz.cast(false);
            } else if (clazz == char.class || clazz == Character.class) {
                return result != null ? result : clazz.cast('\0');
            }
        }
        return result; // 其他類型返回 null
    }
}