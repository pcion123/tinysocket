package com.vscodelife.socketio.buffer;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;

public class JsonMapBuffer implements Cloneable {

    @JSONField(serialize = false, deserialize = false)
    private JSONObject buffer;

    private JsonMapBuffer(JSONObject buffer) {
        this.buffer = buffer;
    }

    @JSONCreator
    public JsonMapBuffer(String json) {
        this.buffer = JSONObject.parseObject(json);
    }

    public JsonMapBuffer() {
        this.buffer = new JSONObject();
    }

    @Override
    public JsonMapBuffer clone() {
        try {
            JsonMapBuffer cloned = new JsonMapBuffer(new JSONObject(this.buffer));
            return cloned;
        } catch (Exception e) {
            throw new RuntimeException("克隆緩衝區時發生錯誤: " + e.getMessage(), e);
        }
    }

    @JSONField(serialize = false)
    public JSONObject getBuffer() {
        return buffer;
    }

    @Override
    public String toString() {
        return buffer.toJSONString();
    }

    public String toJson() {
        return buffer.toJSONString();
    }

    public void setBuffer(String json) {
        this.buffer = JSONObject.parseObject(json);
    }

    public void put(String key, Object value) {
        buffer.put(key, value);
    }

    @JSONField(serialize = false)
    public java.math.BigDecimal getBigDecimal(String key) {
        return buffer.getBigDecimal(key);
    }

    @JSONField(serialize = false)
    public java.math.BigInteger getBigInteger(String key) {
        return buffer.getBigInteger(key);
    }

    @JSONField(serialize = false)
    public boolean getBoolean(String key) {
        return buffer.getBooleanValue(key);
    }

    @JSONField(serialize = false)
    public byte getByte(String key) {
        return buffer.getByteValue(key);
    }

    @JSONField(serialize = false)
    public byte[] getBytes(String key) {
        return buffer.getBytes(key);
    }

    @JSONField(serialize = false)
    public java.util.Date getDate(String key) {
        return buffer.getDate(key);
    }

    @JSONField(serialize = false)
    public double getDouble(String key) {
        return buffer.getDoubleValue(key);
    }

    @JSONField(serialize = false)
    public float getFloat(String key) {
        return buffer.getFloatValue(key);
    }

    @JSONField(serialize = false)
    public int getInteger(String key) {
        return buffer.getIntValue(key);
    }

    @JSONField(serialize = false)
    public long getLong(String key) {
        return buffer.getLongValue(key);
    }

    @JSONField(serialize = false)
    public short getShort(String key) {
        return buffer.getShortValue(key);
    }

    @JSONField(serialize = false)
    public String getString(String key) {
        return buffer.getString(key);
    }
}
