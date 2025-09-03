package com.vscodelife.socketio.util;

import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

/**
 * JSON工具類
 * 基於FastJSON 2.x實現
 */
public final class JsonUtil {

    // 私有建構函數，防止實例化
    private JsonUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 將物件序列化為JSON字串（包含null值）
     * 
     * @param object 要序列化的物件
     * @return JSON字串
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        // 使用 WriteNulls 特性來包含 null 值
        return JSON.toJSONString(object, JSONWriter.Feature.WriteNulls);
    }

    /**
     * 將JSON字串反序列化為指定類型的物件
     * 
     * @param jsonString JSON字串
     * @param clazz      目標類型
     * @return 反序列化後的物件
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    /**
     * 將JSON物件轉換為指定類型的物件
     * 
     * @param jsonObject JSON物件
     * @param clazz      目標類型
     * @return 轉換後的物件
     */
    public static <T> T fromJson(JSONObject jsonObject, Class<T> clazz) {
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(clazz);
    }

    /**
     * 將JSON字串解析為JSONObject
     * 
     * @param jsonString JSON字串
     * @return JSONObject物件
     */
    public static JSONObject parseObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        return JSON.parseObject(jsonString);
    }

    /**
     * 檢查字串是否為有效的JSON格式
     * 
     * @param jsonString 要檢查的字串
     * @return true如果是有效JSON，否則false
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            JSON.parseObject(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        // 測試序列化
        String json = toJson(Map.of("key", "value"));
        System.out.println("Serialized JSON: " + json);

        // 測試反序列化
        Map<String, String> map = fromJson(json, Map.class);
        System.out.println("Deserialized Map: " + map);

        // 測試有效JSON檢查
        boolean isValid = isValidJson(json);
        System.out.println("Is valid JSON: " + isValid);
    }
}
