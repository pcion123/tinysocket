package com.vscodelife.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.client.TestByteClient;
import com.vscodelife.demo.client.component.CommandManager;
import com.vscodelife.demo.constant.ProtocolId;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;

public class DemoByteClient {
    private static final Logger logger = LoggerFactory.getLogger(DemoByteClient.class);

    // 用戶管理器
    private static final CommandManager commandManager = CommandManager.getInstance();

    public static void main(String[] args) {
        TestByteClient client = new TestByteClient("U002", "pass456");
        client.connect("127.0.0.1", 30001);
        // 創建命令管理器
        commandManager.registerCommand("/users", (params) -> {
            ByteArrayBuffer buffer = new ByteArrayBuffer();
            client.send(ProtocolId.GET_USER_LIST, buffer);
            System.out.println("✓ 正在獲取用戶列表...");
        });
        commandManager.registerCommand("/say", (params) -> {
            String message = (String) params;
            ByteArrayBuffer buffer = new ByteArrayBuffer();
            buffer.writeString(message);
            client.send(ProtocolId.SAY, buffer);
            // 顯示簡潔的發送確認，而不是原始訊息
            System.out.println("✓ 訊息已發送");
        }, true);

        try {
            // 啟動交互循環，這會阻塞直到用戶選擇退出
            commandManager.run();
        } finally {
            // 確保客戶端資源得到正確清理
            try {
                client.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("客戶端已退出");
    }
}
