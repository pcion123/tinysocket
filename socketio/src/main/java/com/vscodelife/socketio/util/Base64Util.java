package com.vscodelife.socketio.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Base64Util {

    private Base64Util() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String encode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        byte[] decodedBytes = Base64.getDecoder().decode(input);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
