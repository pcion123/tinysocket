package com.vscodelife.socketio.message.base;

public abstract class MessageBase<T> {
    protected HeaderBase header;
    protected T buffer;

    protected MessageBase() {

    }

    protected MessageBase(HeaderBase header, T buffer) {
        this.header = header;
        this.buffer = buffer;
    }

    @Override
    public String toString() {
        return header.toString();
    }

    @SuppressWarnings("rawtypes")
    public abstract MessageBase clone();

    @SuppressWarnings({ "rawtypes", "hiding" })
    public <T extends MessageBase> T clone(Class<T> clazz) {
        return clazz.cast(clone());
    }

    @SuppressWarnings("hiding")
    public <T extends HeaderBase> T getHeader(Class<T> clazz) {
        return clazz.cast(header);
    }

    public HeaderBase getHeader() {
        return header;
    }

    public void setHeader(HeaderBase header) {
        this.header = header;
    }

    public T getBuffer() {
        return buffer;
    }

    public void setBuffer(T buffer) {
        this.buffer = buffer;
    }

}
