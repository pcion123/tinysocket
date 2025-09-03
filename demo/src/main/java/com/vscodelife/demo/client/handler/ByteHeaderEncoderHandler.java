package com.vscodelife.demo.client.handler;

import java.nio.charset.StandardCharsets;

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
import io.netty.handler.codec.MessageToByteEncoder;

// [Total Length (4 bytes)] + [Header Length (4 bytes)] + [Header JSON] + [Body Length (4 bytes)] + [Body Data]
public class ByteHeaderEncoderHandler extends MessageToByteEncoder<ByteMessage<ByteUserHeader>> {
    private static final Logger logger = LoggerFactory.getLogger(ByteHeaderEncoderHandler.class);

    private final Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector;

    public ByteHeaderEncoderHandler(Connector<ByteUserHeader, ByteMessage<ByteUserHeader>, ByteArrayBuffer> connector) {
        this.connector = connector;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteMessage<ByteUserHeader> message, ByteBuf out)
            throws Exception {
        if (message == null) {
            throw new IllegalArgumentException("Socket or message cannot be null");
        }
        try {
            // 取出header
            ByteUserHeader header = message.getHeader();
            if (header == null) {
                throw new IllegalArgumentException("Header cannot be null");
            }

            // 將header轉換為JSON字串
            String headerJson = JsonUtil.toJson(header);
            // 將JSON字串轉換為byte數組
            byte[] headerBytes = headerJson.getBytes(StandardCharsets.UTF_8);
            // 計算header長度
            int headerLength = headerBytes.length;

            // 取出body
            ByteArrayBuffer body = message.getBuffer();
            // 計算body長度
            int bodyLength = body == null ? 0 : body.readableBytes();

            // 計算總長度： 總長度本身(4) + Header長度字段(4) + Header內容 + Body長度字段(4) + Body內容
            int totalLength = 4 + 4 + headerLength + 4 + bodyLength;

            // 寫入總長度
            NettyUtil.writeInt(out, totalLength);
            // 進行header編碼
            encodeHeader(out, headerBytes);
            // 進行body編碼
            encodeBody(out, body);

            logger.debug(
                    "Encoded message to client {} -> mainNo={} subNo={} requestId={} headerSize={} bodySize={} totalSize={}",
                    ctx.channel().id(), header.getMainNo(), header.getSubNo(),
                    header.getRequestId(), headerLength, bodyLength, totalLength);

            // 通知連接器發送消息事件
            connector.onSendMessage(ctx, message);
        } catch (Exception e) {
            logger.error("Error encoding byte message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void encodeHeader(ByteBuf out, byte[] headerBytes) {
        try {
            NettyUtil.writeBytes(out, headerBytes);
        } catch (Exception e) {
            logger.error("Error encoding header: {}", e.getMessage(), e);
        }
    }

    private void encodeBody(ByteBuf out, ByteArrayBuffer body) {
        try {
            NettyUtil.writeBytesFromByteArrayBuffer(out, body);
        } catch (Exception e) {
            logger.error("Error encoding body: {}", e.getMessage(), e);
        }
    }

}
