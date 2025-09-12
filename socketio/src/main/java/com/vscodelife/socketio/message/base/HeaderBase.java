package com.vscodelife.socketio.message.base;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeaderBase {
    protected String version;
    protected int mainNo;
    protected int subNo;
    protected boolean isCompress;
    protected long sessionId;
    protected long requestId;
    @JSONField(serialize = false, deserialize = false)
    protected long rcvTimestamp;

    @JSONField(serialize = false)
    public ProtocolKey getProtocolKey() {
        return new ProtocolKey(mainNo, subNo);
    }

    @Override
    public String toString() {
        return String.format("version=%s mainNo=%d subNo=%d isCompress=%s sessionId=%d requestId=%d",
                version, mainNo, subNo, isCompress, sessionId, requestId);
    }

    @Override
    public HeaderBase clone() {
        return new HeaderBase(version, mainNo, subNo, isCompress, sessionId, requestId, rcvTimestamp);
    }
}
