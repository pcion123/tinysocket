package com.vscodelife.socketio.message.base;

import java.util.function.Consumer;

public class ProtocolReg<M extends MessageBase<? extends HeaderBase, ?>> {
    private ProtocolKey key;
    private Consumer<M> handler;
    private boolean cached;

    public ProtocolReg(ProtocolKey key, Consumer<M> handler, boolean cached) {
        this.key = key;
        this.handler = handler;
        this.cached = cached;
    }

    public ProtocolKey getKey() {
        return key;
    }

    public Consumer<M> getHandler() {
        return handler;
    }

    public boolean isCached() {
        return cached;
    }
}
