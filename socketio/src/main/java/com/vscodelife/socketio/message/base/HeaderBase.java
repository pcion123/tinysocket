package com.vscodelife.socketio.message.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeaderBase {
    protected int version; // 4
    protected int mainNo; // 8
    protected int subNo; // 12
    protected boolean isCompress; // 13
    protected long sessionId; // 21
    protected int len; // 25

    @Override
    public String toString() {
        return String.format("version=%d mainNo=%d subNo=%d isCompress=%s sessionId=%d len=%d",
                version, mainNo, subNo, isCompress, sessionId, len);
    }

    @Override
    public HeaderBase clone() {
        return new HeaderBase(version, mainNo, subNo, isCompress, sessionId, len);
    }
}
