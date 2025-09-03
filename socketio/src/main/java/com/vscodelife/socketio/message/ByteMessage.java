package com.vscodelife.socketio.message;

import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;

public class ByteMessage<H extends HeaderBase> extends MessageBase<H, ByteArrayBuffer> {
    public ByteMessage() {
        super();
    }

    public ByteMessage(H header, ByteArrayBuffer buffer) {
        super(header, buffer);
    }

    @Override
    public ByteMessage<H> clone() {
        try {
            // 防呆檢查：header 不能為 null
            if (this.header == null) {
                throw new IllegalStateException("Cannot clone ByteMessage: header is null");
            }

            // 防呆檢查：buffer 不能為 null
            if (this.buffer == null) {
                throw new IllegalStateException("Cannot clone ByteMessage: buffer is null");
            }

            // 創建新的 ByteMessage 實例
            ByteMessage<H> cloned = new ByteMessage<H>();

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
            throw new RuntimeException("Failed to clone ByteMessage: " + e.getMessage(), e);
        }
    }
}
