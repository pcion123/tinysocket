package com.vscodelife.demo.webserver;

import com.vscodelife.serversocket.connection.JsonConnection;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.JsonMessage;

import io.netty.channel.Channel;

/**
 * 聊天服務器的用戶連接類
 */
public class ChatUserConnection extends JsonConnection {

    // 默認超時時間：10分鐘
    private static final long DEFAULT_TIMEOUT = 10 * 60 * 1000L;

    private String token; // 認證令牌
    private String userId; // 用戶ID
    private boolean authed; // 是否已驗證
    private long timeout; // 連接超時時間

    public ChatUserConnection() {
        super();
        this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
    }

    public ChatUserConnection(Channel channel, String version, long sessionId, long connectTime) {
        super(channel, version, sessionId, connectTime);
        this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
    }

    @Override
    public void setConnectTime(long connectTime) {
        super.setConnectTime(connectTime);
        timeout = connectTime + DEFAULT_TIMEOUT;
    }

    /**
     * 檢查連接是否超時
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() > timeout;
    }

    /**
     * 更新連接活動時間
     */
    public void updateActivity() {
        this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
    }

    // Getter 和 Setter 方法
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        setProperty(String.class, channel, "userId", userId);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        setProperty(String.class, channel, "token", token);
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;

        setProperty(Boolean.class, channel, "authed", authed);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void send(int mainNo, int subNo, long requestId, JsonMapBuffer buffer) {
        try {
            if (channel != null) {
                channel.writeAndFlush(
                        pack(version, mainNo, subNo, sessionId, userId, token, requestId, buffer));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private JsonMessage<ChatUserHeader> pack(String version, int mainNo, int subNo, long sessionId, String userId,
            String token, long requestId, JsonMapBuffer buffer) {
        String ip = getIp();
        // 產生header
        ChatUserHeader header = new ChatUserHeader(version, mainNo, subNo, false,
                sessionId, requestId, userId, token, ip);
        return new JsonMessage<>(header, buffer);
    }
}
