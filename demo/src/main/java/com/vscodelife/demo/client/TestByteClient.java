package com.vscodelife.demo.client;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.ByteSocket;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.ChannelHandlerContext;

public class TestByteClient extends ByteSocket<ByteUserHeader> {
    private static final Logger logger = LoggerFactory.getLogger(TestByteClient.class);

    private final String userId;
    private final String password;

    private final AtomicBoolean authed = new AtomicBoolean(false);
    private String token;

    public TestByteClient(String userId, String password) {
        super(logger, ByteInitializer.class);

        this.userId = userId;
        this.password = password;

        ByteProtocol.client = this;

        // 使用註解自動註冊協議處理器
        int protocolCount = protocolRegister.scanAndRegisterProtocols(ByteProtocol.class);

        logger.info("reg protocol count={}", protocolCount);
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public Class<TestByteClient> getSocketClazz() {
        return TestByteClient.class;
    }

    @Override
    public void disconnect() {
        send(com.vscodelife.demo.constant.ProtocolId.OFFLINE, new ByteArrayBuffer());

        super.disconnect();
    }

    @Override
    public void onConnected(long connectorId, ChannelHandlerContext ctx) {
        super.onConnected(connectorId, ctx);

        ByteArrayBuffer request = new ByteArrayBuffer();
        request.writeString(userId);
        request.writeString(password);
        send(ProtocolId.AUTH, request);
    }

    @Override
    public void onDisconnected(long connectorId, ChannelHandlerContext ctx) {
        super.onDisconnected(connectorId, ctx);

        authed.set(false);
        token = null;
    }

    @Override
    public void onSendMessage(long connectorId, ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> message) {
        super.onSendMessage(connectorId, ctx, message);
    }

    @Override
    public void onReceiveMessage(long connectorId, ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> message) {
        super.onReceiveMessage(connectorId, ctx, message);
    }

    @Override
    public void onException(long connectorId, ChannelHandlerContext ctx, Throwable cause) {
        super.onException(connectorId, ctx, cause);
    }

    @Override
    protected ByteMessage<ByteUserHeader> pack(String version, int mainNo, int subNo, long sessionId, long requestId,
            ByteArrayBuffer buffer) {
        // 檢查是否需要壓縮
        boolean isCompress = buffer.readableBytes() > 3000;
        if (isCompress) {
            buffer.compress();
        }
        String ip = "127.0.0.1";
        // 產生header
        ByteUserHeader header = new ByteUserHeader(version, mainNo, subNo, isCompress,
                sessionId, requestId, userId, token, ip);
        return new ByteMessage<>(header, buffer);
    }

    public boolean isAuthed() {
        return authed.get();
    }

    public void setAuth(boolean auth) {
        this.authed.set(auth);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
