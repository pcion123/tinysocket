package com.vscodelife.socketio.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.message.base.HeaderBase;

public class ByteCache<H extends HeaderBase> extends CacheBase<ByteMessage<H>, ByteArrayBuffer> {
    private static final Logger logger = LoggerFactory.getLogger(ByteCache.class);

    public ByteCache() {
        super(logger, ByteCache.class.getSimpleName());
    }

    public ByteCache(boolean enabled) {
        super(logger, ByteCache.class.getSimpleName(), enabled);
    }
}
