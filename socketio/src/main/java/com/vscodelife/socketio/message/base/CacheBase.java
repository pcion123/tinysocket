package com.vscodelife.socketio.message.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.vscodelife.socketio.util.DateUtil;

public abstract class CacheBase<M extends MessageBase<? extends HeaderBase, B>, B> {

    private static final long DEFAULT_TIMEOUT = 30000L;
    private static final int DEFAULT_CACHE_SIZE = 10;

    protected final Logger logger;

    private final Set<ProtocolKey> includedKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    protected CacheBase(Logger logger, String clazzName) {
        this(logger, clazzName, true);
    }

    protected CacheBase(Logger logger, String clazzName, boolean enabled) {
        this.logger = logger;
        this.enabled.set(enabled);

        logger.debug("{} cache initialized", clazzName);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enable) {
        this.enabled.set(enable);
    }

    public void update() {
        // 取出過期快取列表
        Set<String> keys = cacheMap.entrySet().stream()
                .filter(entry -> entry.getValue().isTimeout())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        // 依序移除過期快取
        for (String key : keys) {
            cacheMap.remove(key);
            logger.debug("remove key={} and cache map size={}", key, cacheMap.size());
        }
    }

    public boolean isIncluded(M message) {
        return isIncluded(message.getProtocolKey());
    }

    public boolean isIncluded(HeaderBase header) {
        return isIncluded(header.getProtocolKey());
    }

    public boolean isIncluded(ProtocolKey key) {
        return includedKeys.contains(key);
    }

    public void registerProtocolKey(ProtocolKey key) {
        if (!includedKeys.contains(key)) {
            includedKeys.add(key);
        }
    }

    public boolean contains(String key) {
        return cacheMap.containsKey(key);
    }

    public boolean containMessage(String key, M message) {
        Cache cache = cacheMap.get(key);
        if (cache != null) {
            return cache.contains(message);
        }
        return false;
    }

    public void putMessage(String key, M message) {
        ProtocolKey pkey = message.getProtocolKey();
        if (!isIncluded(pkey)) {
            logger.debug("message with key={} is not included in cache", pkey);
            return;
        }
        Cache cache = cacheMap.get(key);
        if (cache == null) {
            cache = new Cache(key);
            cacheMap.put(key, cache);
            logger.debug("put key={} and cache map size={}", key, cacheMap.size());
        }
        cache.put(message);
    }

    public M peekMessage(String key, int mainNo, int subNo, long requestId) {
        Cache cache = cacheMap.get(key);
        if (cache != null) {
            return cache.peek(mainNo, subNo, requestId);
        }
        return null;
    }

    private class Cache {
        private String key;
        private List<M> messages;
        private long timeout;

        public Cache(String key) {
            this.key = key;
            this.messages = new ArrayList<>(DEFAULT_CACHE_SIZE);
            this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
        }

        @Override
        public String toString() {
            return String.format("cache key=%s timeout=%s", key, DateUtil.toTimeStr(timeout));
        }

        public boolean isTimeout() {
            return System.currentTimeMillis() > timeout;
        }

        public boolean contains(M message) {
            synchronized (this) {
                if (messages != null) {
                    ProtocolKey pkey = message.getProtocolKey();
                    long requestId = message.getRequestId();
                    for (M m : messages) {
                        if (m.getProtocolKey().equals(pkey) && m.getRequestId() == requestId) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        public void put(M message) {
            synchronized (this) {
                if (messages != null) {
                    if (DEFAULT_CACHE_SIZE == messages.size()) {
                        messages.remove(0);
                    }
                    //
                    messages.add(message);
                    //
                    timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
                }
            }
        }

        public M peek(int mainNo, int subNo, long requestId) {
            return peek(new ProtocolKey(mainNo, subNo), requestId);
        }

        public M peek(ProtocolKey key, long requestId) {
            synchronized (this) {
                if (messages != null) {
                    for (M m : messages) {
                        if (m.getProtocolKey().equals(key) && m.getRequestId() == requestId) {
                            return m;
                        }
                    }
                }
                return null;
            }
        }
    }
}
