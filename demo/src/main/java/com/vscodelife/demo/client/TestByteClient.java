package com.vscodelife.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.ByteSocket;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.constant.ProtocolId;
import com.vscodelife.socketio.message.ByteMessage;

import io.netty.channel.ChannelHandlerContext;

public class TestByteClient extends ByteSocket<ByteUserHeader> {
    private static final Logger logger = LoggerFactory.getLogger(TestByteClient.class);

    public TestByteClient() {
        super(logger, ByteInitializer.class);

        registerProtocol(ProtocolId.AUTH_RESULT, catchException(message -> authResult(message)));
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
    public void onConnected(long connectorId, ChannelHandlerContext ctx) {
        super.onConnected(connectorId, ctx);
    }

    @Override
    public void onDisconnected(long connectorId, ChannelHandlerContext ctx) {
        super.onDisconnected(connectorId, ctx);
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
        // String ip = getIp();
        String ip = "127.0.0.1";
        String userId = "guest";
        String token = "abcdefg";
        // 產生header
        ByteUserHeader header = new ByteUserHeader(version, mainNo, subNo, isCompress,
                sessionId, requestId, userId, token, ip);
        return new ByteMessage<>(header, buffer);
    }

    protected void authResult(ByteMessage<ByteUserHeader> message) {
        long sessionId = message.getSessionId();
        long requestId = message.getRequestId();
        int code = message.getBuffer().readInt();
        String msg = message.getBuffer().readString();
        logger.info("sessionId={} requestId={} rcv server notify auth result, code={}, msg={}", sessionId, requestId,
                code, msg);
    }

}
