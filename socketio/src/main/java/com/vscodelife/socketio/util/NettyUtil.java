package com.vscodelife.socketio.util;

import java.nio.charset.StandardCharsets;

import com.vscodelife.socketio.buffer.ByteArrayBuffer;

import io.netty.buffer.ByteBuf;

public final class NettyUtil {
    // 私有建構函數，防止實例化
    private NettyUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========== Integer 操作 ==========

    /**
     * 讀取整數（預設使用 Big-Endian）
     */
    public static int readInt(ByteBuf buffer) {
        return readInt(buffer, true);
    }

    /**
     * 讀取整數
     * 
     * @param buffer    ByteBuf
     * @param bigEndian 是否使用 Big-Endian
     */
    public static int readInt(ByteBuf buffer, boolean bigEndian) {
        return bigEndian ? buffer.readInt() : buffer.readIntLE();
    }

    /**
     * 寫入整數（預設使用 Big-Endian）
     */
    public static void writeInt(ByteBuf buffer, int value) {
        writeInt(buffer, value, true);
    }

    /**
     * 寫入整數
     * 
     * @param buffer    ByteBuf
     * @param value     要寫入的整數值
     * @param bigEndian 是否使用 Big-Endian
     */
    public static void writeInt(ByteBuf buffer, int value, boolean bigEndian) {
        if (bigEndian) {
            buffer.writeInt(value);
        } else {
            buffer.writeIntLE(value);
        }
    }

    // ========== Byte Array 操作 ==========

    /**
     * 讀取字節陣列（預設使用 Big-Endian）
     */
    public static byte[] readBytes(ByteBuf buffer) {
        return readBytes(buffer, true);
    }

    /**
     * 讀取字節陣列
     * 
     * @param buffer    ByteBuf
     * @param bigEndian 是否使用 Big-Endian
     */
    public static byte[] readBytes(ByteBuf buffer, boolean bigEndian) {
        byte[] bytes = null;
        if (buffer != null) {
            int len = readInt(buffer, bigEndian);
            if (len > 0) {
                bytes = new byte[len];
                buffer.readBytes(bytes);
            }
        }
        return bytes;
    }

    /**
     * 寫入字節陣列（預設使用 Big-Endian）
     */
    public static void writeBytes(ByteBuf buffer, byte[] bytes) {
        writeBytes(buffer, bytes, true);
    }

    /**
     * 寫入字節陣列
     * 
     * @param buffer    ByteBuf
     * @param bytes     要寫入的字節陣列
     * @param bigEndian 是否使用 Big-Endian
     */
    public static void writeBytes(ByteBuf buffer, byte[] bytes, boolean bigEndian) {
        if (buffer != null) {
            if (bytes == null || bytes.length == 0) {
                writeInt(buffer, 0, bigEndian);
            } else {
                writeInt(buffer, bytes.length, bigEndian);
                buffer.writeBytes(bytes);
            }
        }
    }

    // ========== ByteArrayBuffer 操作 ==========

    /**
     * 讀取字節陣列並轉換為 ByteArrayBuffer（預設使用 Big-Endian）
     */
    public static ByteArrayBuffer readBytesToByteArrayBuffer(ByteBuf buffer) {
        return readBytesToByteArrayBuffer(buffer, true);
    }

    /**
     * 讀取字節陣列並轉換為 ByteArrayBuffer
     * 
     * @param buffer    ByteBuf
     * @param bigEndian 是否使用 Big-Endian
     */
    public static ByteArrayBuffer readBytesToByteArrayBuffer(ByteBuf buffer, boolean bigEndian) {
        ByteArrayBuffer byteArrayBuffer = null;
        if (buffer != null) {
            byte[] bytes = readBytes(buffer, bigEndian);
            if (bytes != null && bytes.length > 0) {
                byteArrayBuffer = new ByteArrayBuffer(bytes,
                        bigEndian ? ByteArrayBuffer.ByteOrder.BIG_ENDIAN : ByteArrayBuffer.ByteOrder.LITTLE_ENDIAN);
            } else {
                byteArrayBuffer = new ByteArrayBuffer();
            }
        }
        return byteArrayBuffer;
    }

    /**
     * 從 ByteArrayBuffer 寫入字節陣列（預設使用 Big-Endian）
     */
    public static void writeBytesFromByteArrayBuffer(ByteBuf buffer, ByteArrayBuffer byteArrayBuffer) {
        writeBytesFromByteArrayBuffer(buffer, byteArrayBuffer, true);
    }

    /**
     * 從 ByteArrayBuffer 寫入字節陣列
     * 
     * @param buffer          ByteBuf
     * @param byteArrayBuffer 來源 ByteArrayBuffer
     * @param bigEndian       是否使用 Big-Endian
     */
    public static void writeBytesFromByteArrayBuffer(ByteBuf buffer, ByteArrayBuffer byteArrayBuffer,
            boolean bigEndian) {
        if (buffer != null) {
            byte[] bytes = byteArrayBuffer == null ? null : byteArrayBuffer.toBytes();
            writeBytes(buffer, bytes, bigEndian);
        }
    }

    // ========== String 操作 ==========

    /**
     * 讀取字串（預設使用 Big-Endian）
     */
    public static String readString(ByteBuf buffer) {
        return readString(buffer, true);
    }

    /**
     * 讀取字串
     * 
     * @param buffer    ByteBuf
     * @param bigEndian 是否使用 Big-Endian
     */
    public static String readString(ByteBuf buffer, boolean bigEndian) {
        String str = "";
        if (buffer != null) {
            byte[] bytes = readBytes(buffer, bigEndian);
            if (bytes != null && bytes.length > 0) {
                str = new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return str;
    }

    /**
     * 寫入字串（預設使用 Big-Endian）
     */
    public static void writeString(ByteBuf buffer, String value) {
        writeString(buffer, value, true);
    }

    /**
     * 寫入字串
     * 
     * @param buffer    ByteBuf
     * @param value     要寫入的字串
     * @param bigEndian 是否使用 Big-Endian
     */
    public static void writeString(ByteBuf buffer, String value, boolean bigEndian) {
        if (buffer != null) {
            if (StrUtil.isEmpty(value)) {
                writeInt(buffer, 0, bigEndian);
            } else {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                writeBytes(buffer, bytes, bigEndian);
            }
        }
    }
}
