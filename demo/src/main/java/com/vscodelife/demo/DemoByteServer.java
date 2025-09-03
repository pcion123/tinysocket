package com.vscodelife.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.TestByteServer;
import com.vscodelife.demo.server.component.ChatManager;
import com.vscodelife.demo.server.component.UserManager;

public class DemoByteServer {
    private static final Logger logger = LoggerFactory.getLogger(DemoByteServer.class);

    // 用戶管理器
    private static final UserManager userManager = UserManager.getInstance();

    // 聊天管理器
    private static final ChatManager chatManager = ChatManager.getInstance();

    public static void main(String[] args) {
        // 啟動服務器
        TestByteServer server = new TestByteServer(30001, 100);
        server.bind();

        logger.info("start byte socket start.");
    }
}
