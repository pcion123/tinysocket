package com.vscodelife.socketio.message;

import com.alibaba.fastjson2.annotation.JSONField;
import com.vscodelife.socketio.buffer.JsonMapBuffer;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

public class JsonMessage<H extends HeaderBase> extends MessageBase<H, JsonMapBuffer> {
    public JsonMessage() {
        super();
    }

    public JsonMessage(H header, JsonMapBuffer buffer) {
        super(header, buffer);
    }

    @JSONField(name = "buffer")
    public String getBufferAsString() {
        return buffer != null ? buffer.toJson() : null;
    }

    @Override
    @JSONField(serialize = false)
    public JsonMapBuffer getBuffer() {
        return super.getBuffer();
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

            // 克隆 buffer
            cloned.setBuffer(this.buffer.clone());

            return cloned;
        } catch (IllegalStateException e) {
            // 重新拋出防呆錯誤
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone JsonMessage: " + e.getMessage(), e);
        }
    }
}
