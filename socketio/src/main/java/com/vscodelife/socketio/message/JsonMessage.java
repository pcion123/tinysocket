package com.vscodelife.socketio.message;

import com.alibaba.fastjson2.JSONObject;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

public class JsonMessage<H extends HeaderBase> extends MessageBase<H, String> {
    private JSONObject jsonBody;

    public JsonMessage() {
        super();
    }

    public JsonMessage(H header, String buffer) {
        super(header, buffer);
    }

    @Override
    public JsonMessage<H> clone() {
        try {
            // 防呆檢查：header 不能為 null
            if (this.header == null) {
                throw new IllegalStateException("Cannot clone JsonMessage: header is null");
            }

            // 防呆檢查：buffer 不能為 null
            if (this.buffer == null) {
                throw new IllegalStateException("Cannot clone JsonMessage: buffer is null");
            }

            // 創建新的 JsonMessage 實例
            JsonMessage<H> cloned = new JsonMessage<H>();

            // 克隆 header
            @SuppressWarnings("unchecked")
            H clonedHeader = (H) this.header.clone();
            cloned.setHeader(clonedHeader);

            // 克隆 buffer（String 是不可變的，直接賦值即可）
            cloned.setBuffer(this.buffer);

            return cloned;
        } catch (IllegalStateException e) {
            // 重新拋出防呆錯誤
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone JsonMessage: " + e.getMessage(), e);
        }
    }

    public int getIntValue(String key) {
        if (jsonBody == null) {
            jsonBody = JSONObject.parseObject(getBuffer());
        }
        return jsonBody.getIntValue(key);
    }

    public String getStringValue(String key) {
        if (jsonBody == null) {
            jsonBody = JSONObject.parseObject(getBuffer());
        }
        return jsonBody.getString(key);
    }

    public <T> T parseValue(String key, Class<T> clazz) {
        if (jsonBody == null) {
            jsonBody = JSONObject.parseObject(getBuffer());
        }
        return jsonBody.getObject(key, clazz);
    }
}
