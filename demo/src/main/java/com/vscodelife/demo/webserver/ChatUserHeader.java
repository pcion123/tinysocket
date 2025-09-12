package com.vscodelife.demo.webserver;

import com.vscodelife.socketio.message.base.HeaderBase;

import lombok.Getter;
import lombok.Setter;

/**
 * 聊天服務器的用戶頭部信息
 */
@Getter
@Setter
public class ChatUserHeader extends HeaderBase {
    private String userId; // 用戶ID
    private String token; // 認證令牌
    private String ip; // 客戶端IP

    // 無參構造函數，用於 JSON 反序列化
    public ChatUserHeader() {
        super();
    }

    public ChatUserHeader(String version, int mainNo, int subNo, boolean isCompress, long sessionId, long requestId,
            String userId, String token) {
        this(version, mainNo, subNo, isCompress, sessionId, requestId, userId, token, "");
    }

    public ChatUserHeader(String version, int mainNo, int subNo, boolean isCompress, long sessionId, long requestId,
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
    public ChatUserHeader clone() {
        return new ChatUserHeader(
                getVersion(), getMainNo(), getSubNo(), isCompress(),
                getSessionId(), getRequestId(),
                userId, token, ip);
    }
}
