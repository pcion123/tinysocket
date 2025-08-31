package com.vscodelife.demo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.serversocket.ByteSocket;

public class TestByteServer extends ByteSocket<ByteUserHeader, ByteUserConnection> {
    private static final Logger logger = LoggerFactory.getLogger(TestByteServer.class);

    public TestByteServer(int port, int maxConnectionLimit) {
        super(logger, port, maxConnectionLimit, ByteInitializer.class);

        // registerProtocol(ProtocolId.AUTH_RESULT, catchException(message ->
        // auth(message)));
    }

    @Override
    public Class<TestByteServer> getSocketClazz() {
        return TestByteServer.class;
    }

    @Override
    protected Class<ByteUserConnection> getConnectionClass() {
        return ByteUserConnection.class;
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public void onConnect(long sessionId) {
        logger.debug("onConnect sessionId={}", sessionId);
    }

    @Override
    public void onDisconnect(long sessionId) {
        logger.debug("onDisconnect sessionId={}", sessionId);
    }
}
