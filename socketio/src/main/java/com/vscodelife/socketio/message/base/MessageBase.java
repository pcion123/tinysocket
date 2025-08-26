package com.vscodelife.socketio.message.base;

public abstract class MessageBase<H extends HeaderBase, B> {
    protected H header;
    protected B buffer;

    protected MessageBase() {

    }

    protected MessageBase(H header, B buffer) {
        this.header = header;
        this.buffer = buffer;
    }

    @Override
    public String toString() {
        return header.toString();
    }

    public abstract MessageBase<H, B> clone();

    public H getHeader() {
        return header;
    }

    public void setHeader(H header) {
        this.header = header;
    }

    public String getVersion() {
        return header == null ? null : header.getVersion();
    }

    public ProtocolKey getProtocolKey() {
        return header == null ? null : header.getProtocolKey();
    }

    public boolean isCompress() {
        return header != null && header.isCompress();
    }

    public long getSessionId() {
        return header == null ? 0L : header.getSessionId();
    }

    public long getRequestId() {
        return header == null ? 0L : header.getRequestId();
    }

    public B getBuffer() {
        return buffer;
    }

    public void setBuffer(B buffer) {
        this.buffer = buffer;
    }

}
