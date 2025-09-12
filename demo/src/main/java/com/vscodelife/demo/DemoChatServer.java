package com.vscodelife.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.webserver.ChatWebServer;
import com.vscodelife.demo.webserver.component.ChatManager;
import com.vscodelife.demo.webserver.component.UserManager;

/**
 * JSON聊天服務器啟動類
 */
public class DemoChatServer {
    private static final Logger logger = LoggerFactory.getLogger(DemoChatServer.class);

    // 用戶管理器
    private static final UserManager userManager = UserManager.getInstance();

    // 聊天管理器
    private static final ChatManager chatManager = ChatManager.getInstance();

    public static void main(String[] args) {
        // 啟動JSON聊天服務器
        ChatWebServer server = new ChatWebServer(30002, 100);
        server.bind();

        logger.info("chat server start.");
    }
}
