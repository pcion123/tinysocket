package com.vscodelife.demo.server.component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.demo.entity.ChatMessage;
import com.vscodelife.demo.entity.User;
import com.vscodelife.demo.server.exception.ChatRuntimeException;

/**
 * 聊天管理器
 * 維護最近50則對話記錄和在線用戶列表
 */
public class ChatManager {
    private static final Logger logger = LoggerFactory.getLogger(ChatManager.class);

    // 最大記錄數量
    private static final int MAX_RECORDS = 50;

    // 聊天記錄列表 (線程安全)
    private final List<ChatMessage> chatRecords = new ArrayList<>();

    // 在線用戶列表 (線程安全) key=userId, value=User
    private final Map<String, User> onlineUsers = new HashMap<>();

    // 讀寫鎖，確保線程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 單例模式
    private static volatile ChatManager instance;

    private ChatManager() {

    }

    /**
     * 獲取單例實例
     */
    public static ChatManager getInstance() {
        if (instance == null) {
            synchronized (ChatManager.class) {
                if (instance == null) {
                    instance = new ChatManager();
                }
            }
        }
        return instance;
    }

    /**
     * 添加聊天記錄
     * 
     * @param userId   用戶ID
     * @param userName 用戶名稱
     * @param content  訊息內容
     */
    public ChatMessage addMessage(String userId, String content) {
        ChatMessage message = null;
        lock.writeLock().lock();
        try {
            User user = onlineUsers.get(userId);
            if (user == null) {
                throw new ChatRuntimeException("用戶未登錄: " + userId);
            }
            String userName = user.getUserName();

            message = ChatMessage.createUserMessage(userId, userName, content);

            chatRecords.add(message);

            // 如果超過最大記錄數，移除最舊的記錄
            if (chatRecords.size() > MAX_RECORDS) {
                chatRecords.remove(0);
            }

            logger.info("添加聊天記錄: {} - {}: {}", userName, userId, content);
        } finally {
            lock.writeLock().unlock();
        }
        return message;
    }

    /**
     * 添加聊天記錄
     * 
     * @param message 聊天訊息物件
     */
    public void addMessage(ChatMessage message) {
        lock.writeLock().lock();
        try {
            chatRecords.add(message);

            // 如果超過最大記錄數，移除最舊的記錄
            if (chatRecords.size() > MAX_RECORDS) {
                chatRecords.remove(0);
            }

            logger.info("添加聊天記錄: {}", message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取所有聊天記錄 (不可變列表)
     * 
     * @return 聊天記錄列表的副本
     */
    public List<ChatMessage> getAllMessages() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(chatRecords));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取指定用戶的聊天記錄
     * 
     * @param userId 用戶ID
     * @return 該用戶的聊天記錄列表
     */
    public List<ChatMessage> getMessagesByUser(String userId) {
        lock.readLock().lock();
        try {
            return chatRecords.stream()
                    .filter(message -> message.getUserId().equals(userId))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取最近N條記錄
     * 
     * @param count 記錄數量
     * @return 最近的記錄列表
     */
    public List<ChatMessage> getRecentMessages(int count) {
        lock.readLock().lock();
        try {
            int size = chatRecords.size();
            int startIndex = Math.max(0, size - count);
            return Collections.unmodifiableList(
                    new ArrayList<>(chatRecords.subList(startIndex, size)));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空所有聊天記錄
     */
    public void clearAllMessages() {
        lock.writeLock().lock();
        try {
            chatRecords.clear();
            logger.info("已清空所有聊天記錄");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取記錄總數
     * 
     * @return 記錄數量
     */
    public int getMessageCount() {
        lock.readLock().lock();
        try {
            return chatRecords.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取時間範圍內的記錄
     * 
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 時間範圍內的記錄列表
     */
    public List<ChatMessage> getMessagesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        lock.readLock().lock();
        try {
            return chatRecords.stream()
                    .filter(message -> {
                        LocalDateTime timestamp = message.getTimestamp();
                        return !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime);
                    })
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 打印所有聊天記錄
     */
    public void printAllMessages() {
        lock.readLock().lock();
        try {
            System.out.println("=== 聊天記錄 (共 " + chatRecords.size() + " 條) ===");
            for (ChatMessage message : chatRecords) {
                System.out.println(message);
            }
            System.out.println("========================");
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 在線用戶管理 ====================

    /**
     * 用戶上線
     * 
     * @param user 用戶對象
     */
    public void userOnline(User user) {
        lock.writeLock().lock();
        try {
            onlineUsers.put(user.getUserId(), user);
            logger.info("用戶上線: {} - {}", user.getUserId(), user.getUserName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 用戶下線
     * 
     * @param userId 用戶ID
     */
    public User userOffline(String userId) {
        User removedUser = null;
        lock.writeLock().lock();
        try {
            removedUser = onlineUsers.remove(userId);
            if (removedUser != null) {
                logger.info("用戶下線: {} - {}", userId, removedUser.getUserName());
            } else {
                logger.warn("嘗試下線不存在的用戶: {}", userId);
            }
        } finally {
            lock.writeLock().unlock();
        }
        return removedUser;
    }

    /**
     * 檢查用戶是否在線
     * 
     * @param userId 用戶ID
     * @return 是否在線
     */
    public boolean isUserOnline(String userId) {
        lock.readLock().lock();
        try {
            return onlineUsers.containsKey(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取在線用戶
     * 
     * @param userId 用戶ID
     * @return 用戶對象，如果不在線則返回null
     */
    public User getOnlineUser(String userId) {
        lock.readLock().lock();
        try {
            return onlineUsers.get(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有在線用戶 (不可變Map)
     * 
     * @return 在線用戶Map的副本
     */
    public List<User> getAllOnlineUsers() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(onlineUsers.values()));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取在線用戶數量
     * 
     * @return 在線用戶數量
     */
    public int getOnlineUserCount() {
        lock.readLock().lock();
        try {
            return onlineUsers.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有在線用戶ID列表
     * 
     * @return 在線用戶ID列表
     */
    public List<String> getOnlineUserIds() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(onlineUsers.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有在線用戶名稱列表
     * 
     * @return 在線用戶名稱列表
     */
    public List<String> getOnlineUserNames() {
        lock.readLock().lock();
        try {
            return onlineUsers.values().stream()
                    .map(User::getUserName)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空所有在線用戶
     */
    public void clearAllOnlineUsers() {
        lock.writeLock().lock();
        try {
            int count = onlineUsers.size();
            onlineUsers.clear();
            logger.info("已清空所有在線用戶 (共{}個)", count);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 打印所有在線用戶
     */
    public void printAllOnlineUsers() {
        lock.readLock().lock();
        try {
            System.out.println("=== 在線用戶 (共 " + onlineUsers.size() + " 人) ===");
            for (User user : onlineUsers.values()) {
                System.out.println(String.format("ID: %s, 姓名: %s, 性別: %s, 年齡: %d, 職業: %s",
                        user.getUserId(), user.getUserName(), user.getGender(),
                        user.getAge(), user.getOccupation()));
            }
            System.out.println("========================");
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== 組合操作 ====================

    /**
     * 用戶上線並發送上線訊息
     * 
     * @param user 用戶對象
     */
    public ChatMessage userOnlineWithMessage(User user) {
        ChatMessage message = null;
        //
        userOnline(user);
        //
        message = ChatMessage.createSystemMessage(String.format("%s 已上線", user.getUserName()));
        //
        addMessage(message);

        return message;
    }

    /**
     * 用戶下線並發送下線訊息
     * 
     * @param userId 用戶ID
     */
    public ChatMessage userOfflineWithMessage(String userId) {
        ChatMessage message = null;
        User user = userOffline(userId);
        if (user != null) {
            //
            message = ChatMessage.createSystemMessage(String.format("%s 已下線", user.getUserName()));
            //
            addMessage(message);
        }
        return message;
    }

    /**
     * 獲取統計信息
     * 
     * @return 統計信息字符串
     */
    public String getStatistics() {
        lock.readLock().lock();
        try {
            return String.format("聊天記錄: %d 條, 在線用戶: %d 人",
                    chatRecords.size(), onlineUsers.size());
        } finally {
            lock.readLock().unlock();
        }
    }
}
