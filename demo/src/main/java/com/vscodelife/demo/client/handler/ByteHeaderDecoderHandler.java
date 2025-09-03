package com.vscodelife.demo.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.clientsocket.Connector;
import com.vscodelife.demo.client.ByteUserHeader;
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.message.ByteMessage;
import com.vscodelife.socketio.util.JsonUtil;
import com.vscodelife.socketio.util.NettyUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;

// [Total Length (4 bytes)] + [Header Length (4 bytes)] + [Header JSON] + [Body Length (4 bytes)] + [Body Data]
public class ByteHeaderDecoderHandler extends LengthFieldBasedFrameDecoder {
    private static final Logger logger = LoggerFactory.getLogger(ByteHeaderDecoderHandler.class);

    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 0; // 總長度字段在開頭
    private static final int LENGTH_FIELD_LENGTH = 4; // 總長度字段佔 4 個字節
    private static final int LENGTH_ADJUSTMENT = -4; // 長度調整：總長度包含自身，所以要減去 4
    private static final int INITIAL_BYTES_TO_STRIP = 4; // 跳過總長度字段

    private final Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector;

    public ByteHeaderDecoderHandler(Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector) {
        super(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT,
                INITIAL_BYTES_TO_STRIP);

        this.connector = connector;
    }

    @Override
    protected ByteMessage<ByteUserHeader> decode(ChannelHandlerContext ctx, ByteBuf pre)
            throws Exception {
        // 使用父類的幀解碼，自動處理粘包拆包
        ByteBuf in = (ByteBuf) super.decode(ctx, pre);
        if (in == null) {
            return null; // 等待更多數據
        }
        ByteMessage<ByteUserHeader> message = null;
        try {
            if (in.readableBytes() > 0) {
                // 解析完整的幀數據
                message = decodeFrame(ctx, in);
            } else {
                logger.debug("No readable bytes");
            }
        } catch (Exception e) {
            logger.error("Error decoding byte message from channel {}: {}",
                    ctx.channel().id(), e.getMessage(), e);
        } finally {
            ReferenceCountUtil.release(in);
        }
        return message;
    }

    /**
     * 解析完整幀：Header JSON + Body Data
     */
    private ByteMessage<ByteUserHeader> decodeFrame(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.readableBytes() < 4) {
            // 這種情況理論上不應該發生，因為 LengthFieldBasedFrameDecoder 已經確保了數據完整性
            logger.error(
                    "Protocol error: Frame too small, expected at least 4 bytes for header length, but got {} bytes",
                    in.readableBytes());
            throw new IllegalStateException("Protocol violation: incomplete frame data");
        }

        // 解析 Header
        ByteUserHeader header = decodeJsonHeader(in);
        if (header == null) {
            logger.error("Failed to decode header JSON - protocol violation or corrupted data");
            throw new IllegalStateException("Header parsing failed - invalid JSON format or corrupted data");
        }

        // 檢查是否有足夠的字節來讀取 Body Length
        if (in.readableBytes() < 4) {
            // 這種情況也理論上不應該發生
            logger.error(
                    "Protocol error: Frame too small, expected at least 4 bytes for body length after header, but got {} bytes",
                    in.readableBytes());
            throw new IllegalStateException("Protocol violation: incomplete frame data after header");
        }

        // 解析 Body
        ByteArrayBuffer body = decodeBody(in);
        if (body == null) {
            logger.error("Failed to decode body - protocol violation or corrupted data");
            throw new IllegalStateException("Body parsing failed - invalid format or corrupted data");
        }

        // 創建消息
        ByteMessage<ByteUserHeader> message = new ByteMessage<>(header, body);

        logger.debug(
                "Successfully decoded message from client {} {} -> mainNo={} subNo={} requestId={} bodySize={}",
                ctx.channel().remoteAddress(), ctx.channel().id(),
                header.getMainNo(), header.getSubNo(), header.getRequestId(),
                body.readableBytes());

        return message;
    }

    /**
     * 解析 JSON Header
     */
    private ByteUserHeader decodeJsonHeader(ByteBuf in) {
        try {
            String headerJson = NettyUtil.readString(in);
            logger.debug("Received header JSON: {}", headerJson);
            ByteUserHeader header = JsonUtil.fromJson(headerJson, ByteUserHeader.class);
            return header;
        } catch (Exception e) {
            logger.error("Error parsing header JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析 Body 數據
     */
    private ByteArrayBuffer decodeBody(ByteBuf in) {
        try {
            return NettyUtil.readBytesToByteArrayBuffer(in);
        } catch (Exception e) {
            logger.error("Error reading body bytes: {}", e.getMessage(), e);
            return null;
        }
    }

}
