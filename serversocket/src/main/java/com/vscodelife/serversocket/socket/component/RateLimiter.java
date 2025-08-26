package com.vscodelife.serversocket.socket.component;

import com.vscodelife.socketio.util.RandomUtil;

public class RateLimiter {
    private static final long DEFAULT_LIMIT_TIME = 10 * 60 * 1000L; // 10 minutes
    private static final int DEFAULT_FILTER_RATE = 20; // 20% pass rate
    private static final int DEFAULT_PASS_RATE = 999; // 999% pass rate

    private boolean enabled;
    private long limitTime;
    private int filterRate;

    public RateLimiter() {
        this(false);
    }

    public RateLimiter(boolean enabled) {
        this(enabled, DEFAULT_LIMIT_TIME, DEFAULT_FILTER_RATE);
    }

    public RateLimiter(boolean enabled, long limitTime, int filterRate) {
        this.enabled = enabled;
        this.limitTime = System.currentTimeMillis() + limitTime;
        this.filterRate = filterRate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getLimitTime() {
        return limitTime;
    }

    public int getFilterRate() {
        return filterRate;
    }

    public void enable() {
        enable(DEFAULT_LIMIT_TIME, DEFAULT_FILTER_RATE);
    }

    public void enable(long limitTime, int filterRate) {
        synchronized (this) {
            this.enabled = true;
            this.limitTime = System.currentTimeMillis() + limitTime;
            this.filterRate = filterRate;
        }
    }

    public void disable() {
        synchronized (this) {
            this.enabled = false;
            this.limitTime = System.currentTimeMillis();
            this.filterRate = DEFAULT_PASS_RATE;
        }
    }

    public boolean pass() {
        final boolean cacheEnabled;
        final long cacheLimitTime;
        final int cacheFilterRate;
        synchronized (this) {
            cacheEnabled = this.enabled;
            cacheLimitTime = this.limitTime;
            cacheFilterRate = this.filterRate;
        }
        if (!cacheEnabled) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime > cacheLimitTime) {
            disable();
            return true;
        }
        int rnd = RandomUtil.randomInt(1, 100);
        return rnd > cacheFilterRate;
    }
}
