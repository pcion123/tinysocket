package com.vscodelife.socketio.util;

public final class RandomUtil {
    // 私有建構函數，防止實例化
    private RandomUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static int randomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Invalid range");
        }
        return (int) (Math.random() * (max - min)) + min;
    }
}
