package com.vscodelife.demo.entity;

import java.time.LocalDateTime;

import com.vscodelife.socketio.annotation.MessageTag;
import com.vscodelife.socketio.util.SnowflakeUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天訊息實體類
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    public static final int TYPE_SYSTEM = 0; // 系統訊息
    public static final int TYPE_USER = 1; // 用戶訊息

    @MessageTag(order = 1)
    private int messageType; // 訊息類型 0=系統訊息 1=用戶訊息
    @MessageTag(order = 2)
    private long messageId; // 訊息ID (使用Snowflake)
    @MessageTag(order = 3)
    private String userId; // 用戶編號
    @MessageTag(order = 4)
    private String userName; // 用戶名稱
    @MessageTag(order = 5)
    private String content; // 訊息內容
    @MessageTag(order = 6)
    private LocalDateTime timestamp; // 時間戳記

    /**
     * 創建系統訊息
     */
    public static ChatMessage createSystemMessage(String content) {
        return new ChatMessage(TYPE_SYSTEM, SnowflakeUtil.nextId(), "SYSTEM", "系統", content, LocalDateTime.now());
    }

    /**
     * 創建用戶訊息
     */
    public static ChatMessage createUserMessage(String userId, String userName, String content) {
        return new ChatMessage(TYPE_USER, SnowflakeUtil.nextId(), userId, userName, content, LocalDateTime.now());
    }

    /**
     * 獲取訊息類型描述
     */
    public String getMessageTypeDescription() {
        return messageType == TYPE_SYSTEM ? "系統訊息" : "用戶訊息";
    }

    /**
     * 是否為系統訊息
     */
    public boolean isSystemMessage() {
        return messageType == TYPE_SYSTEM;
    }

    /**
     * 是否為用戶訊息
     */
    public boolean isUserMessage() {
        return messageType == TYPE_USER;
    }

    @Override
    public String toString() {
        String typePrefix = isSystemMessage() ? "[系統]" : "[用戶]";
        return String.format("%s[%d][%s] %s: %s",
                typePrefix, messageId, timestamp.toString(), userName, content);
    }
}
