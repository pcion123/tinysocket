package com.vscodelife.demo.server.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.entity.User;

/**
 * 用戶管理器
 * 管理系統中的所有用戶資料
 */
public class UserManager {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    // 用戶資料Map (線程安全) key=userId, value=User
    private final Map<String, User> users = new HashMap<>();

    // 讀寫鎖，確保線程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 單例模式
    private static volatile UserManager instance;

    private UserManager() {
        initializeUsers();
    }

    /**
     * 獲取單例實例
     */
    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化用戶資料
     */
    private void initializeUsers() {
        lock.writeLock().lock();
        try {
            // 清空現有用戶
            users.clear();

            // 隨機生成的用戶資料
            addUser(new User("U001", "password123", "張小明", "男", 25, "軟體工程師"));
            addUser(new User("U002", "pass456", "王美麗", "女", 28, "設計師"));
            addUser(new User("U003", "secret789", "李大華", "男", 32, "產品經理"));
            addUser(new User("U004", "mypass321", "陳小芳", "女", 24, "護理師"));
            addUser(new User("U005", "test999", "劉志強", "男", 35, "教師"));
            addUser(new User("U006", "hello888", "黃淑萍", "女", 29, "會計師"));
            addUser(new User("U007", "secure777", "吳建國", "男", 27, "銷售代表"));
            addUser(new User("U008", "admin666", "林雅雯", "女", 31, "行政助理"));
            addUser(new User("U009", "user555", "楊志明", "男", 26, "資料分析師"));
            addUser(new User("U010", "demo444", "許美玲", "女", 30, "市場專員"));

            logger.info("已初始化 {} 個用戶", users.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== 用戶管理 ====================

    /**
     * 添加用戶
     * 
     * @param user 用戶對象
     */
    public void addUser(User user) {
        lock.writeLock().lock();
        try {
            users.put(user.getUserId(), user);
            logger.debug("添加用戶: {} - {}", user.getUserId(), user.getUserName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 根據用戶ID獲取用戶
     * 
     * @param userId 用戶ID
     * @return 用戶對象，不存在則返回null
     */
    public User getUser(String userId) {
        lock.readLock().lock();
        try {
            return users.get(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 檢查用戶是否存在
     * 
     * @param userId 用戶ID
     * @return 是否存在
     */
    public boolean userExists(String userId) {
        lock.readLock().lock();
        try {
            return users.containsKey(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新用戶資訊
     * 
     * @param user 用戶對象
     * @return 是否更新成功
     */
    public boolean updateUser(User user) {
        lock.writeLock().lock();
        try {
            if (users.containsKey(user.getUserId())) {
                users.put(user.getUserId(), user);
                logger.info("更新用戶資訊: {} - {}", user.getUserId(), user.getUserName());
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 刪除用戶
     * 
     * @param userId 用戶ID
     * @return 是否刪除成功
     */
    public boolean removeUser(String userId) {
        lock.writeLock().lock();
        try {
            User removedUser = users.remove(userId);
            if (removedUser != null) {
                logger.info("刪除用戶: {} - {}", userId, removedUser.getUserName());
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取所有用戶 (不可變Map)
     * 
     * @return 所有用戶的副本
     */
    public Map<String, User> getAllUsers() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(users));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取用戶總數
     * 
     * @return 用戶數量
     */
    public int getUserCount() {
        lock.readLock().lock();
        try {
            return users.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有用戶ID列表
     * 
     * @return 用戶ID列表
     */
    public List<String> getAllUserIds() {
        lock.readLock().lock();
        try {
            return users.keySet().stream()
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有用戶名稱列表
     * 
     * @return 用戶名稱列表
     */
    public List<String> getAllUserNames() {
        lock.readLock().lock();
        try {
            return users.values().stream()
                    .map(User::getUserName)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 驗證功能 ====================

    /**
     * 驗證用戶登入
     * 
     * @param userId   用戶ID
     * @param password 密碼
     * @return 驗證成功返回用戶對象，失敗返回null
     */
    public User authenticateUser(String userId, String password) {
        lock.readLock().lock();
        try {
            User user = users.get(userId);
            if (user != null && user.getPassword().equals(password)) {
                logger.info("用戶登入成功: {} - {}", userId, user.getUserName());
                return user;
            }
            logger.warn("用戶登入失敗: {}", userId);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 清空所有用戶
     */
    public void clearAllUsers() {
        lock.writeLock().lock();
        try {
            int count = users.size();
            users.clear();
            logger.info("已清空所有用戶 (共{}個)", count);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 重置為初始用戶資料
     */
    public void resetToInitialUsers() {
        initializeUsers();
    }

    /**
     * 打印所有用戶資訊
     */
    public void printAllUsers() {
        lock.readLock().lock();
        try {
            System.out.println("=== 所有用戶 (共 " + users.size() + " 人) ===");
            for (User user : users.values()) {
                System.out.println(String.format("ID: %s, 姓名: %s, 性別: %s, 年齡: %d, 職業: %s",
                        user.getUserId(), user.getUserName(), user.getGender(),
                        user.getAge(), user.getOccupation()));
            }
            System.out.println("========================");
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取統計信息
     * 
     * @return 統計信息字符串
     */
    public String getStatistics() {
        lock.readLock().lock();
        try {
            long maleCount = users.values().stream().filter(u -> "男".equals(u.getGender())).count();
            long femaleCount = users.values().stream().filter(u -> "女".equals(u.getGender())).count();
            double avgAge = users.values().stream().mapToInt(User::getAge).average().orElse(0);

            return String.format("總用戶數: %d, 男性: %d, 女性: %d, 平均年齡: %.1f",
                    users.size(), maleCount, femaleCount, avgAge);
        } finally {
            lock.readLock().unlock();
        }
    }
}
