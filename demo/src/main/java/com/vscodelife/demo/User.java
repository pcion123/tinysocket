package com.vscodelife.demo;

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
    private String userId; // 用戶編號
    private String password; // 密碼
    private String userName; // 用戶名稱
    private String gender; // 性別
    private int age; // 年齡
    private String occupation; // 職業
}
