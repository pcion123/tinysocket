package com.vscodelife.demo.entity;

import com.vscodelife.socketio.annotation.MessageTag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用戶實體類
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @MessageTag(order = 1)
    private String userId; // 用戶編號
    private String password; // 密碼
    @MessageTag(order = 2)
    private String userName; // 用戶名稱
    @MessageTag(order = 3)
    private String gender; // 性別
    @MessageTag(order = 4)
    private int age; // 年齡
    @MessageTag(order = 5)
    private String occupation; // 職業
}
