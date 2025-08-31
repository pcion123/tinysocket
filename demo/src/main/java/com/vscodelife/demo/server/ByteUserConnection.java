package com.vscodelife.demo.server;

import com.vscodelife.serversocket.connection.ByteConnection;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.Channel;

public class ByteUserConnection extends ByteConnection {
    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000L;

    private String token;
    private String userId;
    private long timeout;

    public ByteUserConnection() {
        super();

        this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
    }

    public ByteUserConnection(Channel channel, String version, long sessionId, long connectTime) {
        super(channel, version, sessionId, connectTime);

        this.timeout = System.currentTimeMillis() + DEFAULT_TIMEOUT;
    }

    @Override
    public void setConnectTime(long connectTime) {
        super.setConnectTime(connectTime);

        timeout = connectTime + DEFAULT_TIMEOUT;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;

        setProperty(String.class, channel, "token", token);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;

        setProperty(String.class, channel, "userId", userId);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void send(int mainNo, int subNo, long requestId, ByteArrayBuffer buffer) {
        try {
            if (channel != null) {
                channel.writeAndFlush(
                        pack(version, mainNo, subNo, sessionId, userId, token, requestId, buffer));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private ByteMessage<ByteUserHeader> pack(String version, int mainNo, int subNo, long sessionId, String userId,
            String token, long requestId, ByteArrayBuffer buffer) {
        // 檢查是否需要壓縮
        boolean isCompress = buffer.readableBytes() > 3000;
        if (isCompress) {
            buffer.compress();
        }
        String ip = getIp();
        // 產生header
        ByteUserHeader header = new ByteUserHeader(version, mainNo, subNo, isCompress,
                sessionId, requestId, userId, token, ip);
        return new ByteMessage<>(header, buffer);
    }
}
