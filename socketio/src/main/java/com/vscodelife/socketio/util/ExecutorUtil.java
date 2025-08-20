package com.vscodelife.socketio.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ExecutorUtil {

    // 私有建構函數，防止實例化
    private ExecutorUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ThreadFactory makeName(final String name) {
        final AtomicLong couter = new AtomicLong(-1);
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("%s-%d", name, couter.incrementAndGet()));
            }
        };
    }
}
