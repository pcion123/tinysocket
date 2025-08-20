package com.vscodelife.socketio.util;

public final class StrUtil {

    // 私有建構函數，防止實例化
    private StrUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 檢查字串是否為空 (null 或空字串)
     * 
     * @param str 要檢查的字串
     * @return 如果字串為 null 或空字串則返回 true，否則返回 false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 檢查字串是否不為空
     * 
     * @param str 要檢查的字串
     * @return 如果字串不為 null 且不為空字串則返回 true，否則返回 false
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 檢查字串是否為空白 (null、空字串或只包含空白字符)
     * 
     * @param str 要檢查的字串
     * @return 如果字串為 null、空字串或只包含空白字符則返回 true，否則返回 false
     */
    public static boolean isBlank(String str) {
        if (isEmpty(str)) {
            return true;
        }
        return str.trim().length() == 0;
    }

    /**
     * 檢查字串是否不為空白
     * 
     * @param str 要檢查的字串
     * @return 如果字串不為 null、不為空字串且包含非空白字符則返回 true，否則返回 false
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
