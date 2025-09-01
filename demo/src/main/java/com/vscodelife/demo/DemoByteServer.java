package com.vscodelife.demo;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.server.TestByteServer;

public class DemoByteServer {
    private static final Logger logger = LoggerFactory.getLogger(DemoByteServer.class);

    // 用戶列表常數 Map，key = userId
    private static final Map<String, User> USERS = createUserMap();

    /**
     * 創建用戶Map，包含隨機生成的用戶資料
     */
    private static Map<String, User> createUserMap() {
        Map<String, User> userMap = new HashMap<>();

        // 隨機生成的用戶資料
        userMap.put("U001", new User("U001", "password123", "張小明", "男", 25, "軟體工程師"));
        userMap.put("U002", new User("U002", "pass456", "王美麗", "女", 28, "設計師"));
        userMap.put("U003", new User("U003", "secret789", "李大華", "男", 32, "產品經理"));
        userMap.put("U004", new User("U004", "mypass321", "陳小芳", "女", 24, "護理師"));
        userMap.put("U005", new User("U005", "test999", "劉志強", "男", 35, "教師"));
        userMap.put("U006", new User("U006", "hello888", "黃淑萍", "女", 29, "會計師"));
        userMap.put("U007", new User("U007", "secure777", "吳建國", "男", 27, "銷售代表"));
        userMap.put("U008", new User("U008", "admin666", "林雅雯", "女", 31, "行政助理"));
        userMap.put("U009", new User("U009", "user555", "楊志明", "男", 26, "資料分析師"));
        userMap.put("U010", new User("U010", "demo444", "許美玲", "女", 30, "市場專員"));

        return userMap;
    }

    /**
     * 根據用戶ID獲取用戶資訊
     */
    public static User getUser(String userId) {
        return USERS.get(userId);
    }

    /**
     * 獲取所有用戶
     */
    public static Map<String, User> getAllUsers() {
        return new HashMap<>(USERS);
    }

    public static void main(String[] args) {
        TestByteServer server = new TestByteServer(30001, 100);
        server.bind();

        logger.info("start byte socket start.");
    }
}
