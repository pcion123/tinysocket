package com.vscodelife.demo.webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.serversocket.JsonSocket;

/**
 * 基於WebSocket的聊天服務器
 */
public class ChatWebServer extends JsonSocket<ChatUserHeader, ChatUserConnection> {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebServer.class);

    public ChatWebServer(int port, int maxConnectionLimit) {
        super(logger, port, maxConnectionLimit, ChatInitializer.class);

        // 設置協議處理器
        ChatProtocol.server = this;

        // 註冊協議處理器
        int protocolCount = protocolRegister.scanAndRegisterProtocols(ChatProtocol.class);

        logger.info("reg protocol count={}", protocolCount);
    }

    @Override
    public Class<ChatWebServer> getSocketClazz() {
        return ChatWebServer.class;
    }

    @Override
    protected Class<ChatUserConnection> getConnectionClass() {
        return ChatUserConnection.class;
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
