package com.vscodelife.demo.server;

import com.vscodelife.socketio.message.base.HeaderBase;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ByteUserHeader extends HeaderBase {
    private String userId;
    private String token;
    private String ip;

    // 無參構造函數，用於 JSON 反序列化
    public ByteUserHeader() {
        super();
    }

    public ByteUserHeader(String version, int mainNo, int subNo, boolean isCompress, long sessionId, long requestId,
            String userId, String token) {
        this(version, mainNo, subNo, isCompress, sessionId, requestId, userId, token, "");
    }

    public ByteUserHeader(String version, int mainNo, int subNo, boolean isCompress, long sessionId, long requestId,
            String userId, String token, String ip) {
        super(version, mainNo, subNo, isCompress, sessionId, requestId, 0L);

        this.userId = userId;
        this.token = token;
        this.ip = ip;
    }

    @Override
    public String toString() {
        return String.format(
                "version=%s mainNo=%d subNo=%d isCompress=%s sessionId=%d requestId=%d userId=%s token=%s ip=%s",
                version, mainNo, subNo, isCompress, sessionId, requestId, userId, token, ip);
    }

    @Override
    public ByteUserHeader clone() {
        return new ByteUserHeader(version, mainNo, subNo, isCompress, sessionId, requestId, userId, token, ip);
    }
}
