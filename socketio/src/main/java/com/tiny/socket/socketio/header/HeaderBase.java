package com.tiny.socket.socketio.header;

public class HeaderBase {
    public final static int LENGTH = 17;

    protected short version;
    protected byte mainNo;
    protected byte subNo;
    protected boolean isCompress;
    protected long sessionId;
    protected int len;

    public HeaderBase() {

    }

    public HeaderBase(short version, byte mainNo, byte subNo, boolean isCompress, long sessionId,
            int len) {
        this.version = version;
        this.mainNo = mainNo;
        this.subNo = subNo;
        this.isCompress = isCompress;
        this.sessionId = sessionId;
        this.len = len;
    }

    @Override
    public String toString() {
        return String.format("version=%d mainNo=%d subNo=%d isCompress=%s sessionId=%d len=%d",
                version, mainNo, subNo, isCompress, sessionId, len);
    }

    public HeaderBase clone() {
        return new HeaderBase(version, mainNo, subNo, isCompress, sessionId, len);
    }

    public short getVersion() {
        return version;
    }

    public void setVersion(short version) {
        this.version = version;
    }

    public byte getMainNo() {
        return mainNo;
    }

    public void setMainNo(byte mainNo) {
        this.mainNo = mainNo;
    }

    public byte getSubNo() {
        return subNo;
    }

    public void setSubNo(byte subNo) {
        this.subNo = subNo;
    }

    public boolean getIsCompress() {
        return isCompress;
    }

    public void setIsCompress(boolean isCompress) {
        this.isCompress = isCompress;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }
}
