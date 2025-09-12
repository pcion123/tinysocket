package com.vscodelife.socketio.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.message.base.HeaderBase;

public class JsonCache<H extends HeaderBase> extends CacheBase<JsonMessage<H>, JsonMapBuffer> {
    private static final Logger logger = LoggerFactory.getLogger(JsonCache.class);

    public JsonCache() {
        super(logger, JsonCache.class.getSimpleName());
    }

    public JsonCache(boolean enabled) {
        super(logger, JsonCache.class.getSimpleName(), enabled);
    }
}
