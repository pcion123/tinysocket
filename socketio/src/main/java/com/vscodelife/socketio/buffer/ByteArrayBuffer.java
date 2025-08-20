package com.vscodelife.socketio.buffer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import com.vscodelife.socketio.annotation.MessageTag;
import com.vscodelife.socketio.util.JsonUtil;

/**
 * 可重複使用的位元組陣列緩衝區
 * 提供類似 Netty ByteBuf 的 API，支援讀寫各種資料型別
 * 支援 Big-Endian（網絡字節序）和 Little-Endian 字節序
 */
public class ByteArrayBuffer implements Cloneable {

    // ==================== 常數定義 ====================

    private static final int DEFAULT_CAPACITY = 256;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    /**
     * 字節序枚舉
     */
    public enum ByteOrder {
        BIG_ENDIAN, // 大端序（網絡字節序）- 預設
        LITTLE_ENDIAN // 小端序（Intel x86 架構）
    }

    // ==================== 實例變數 ====================

    private byte[] buffer;
    private int writeIndex;
    private int readIndex;
    private int capacity;
    private ByteOrder byteOrder;

    // ==================== 建構子 ====================

    /**
     * 建立預設容量的緩衝區（Big-Endian）
     */
    public ByteArrayBuffer() {
        this(DEFAULT_CAPACITY, ByteOrder.BIG_ENDIAN);
    }

    /**
     * 建立指定容量的緩衝區（Big-Endian）
     * 
     * @param initialCapacity 初始容量
     */
    public ByteArrayBuffer(int initialCapacity) {
        this(initialCapacity, ByteOrder.BIG_ENDIAN);
    }

    /**
     * 建立指定容量和字節序的緩衝區
     * 
     * @param initialCapacity 初始容量
     * @param byteOrder       字節序
     */
    public ByteArrayBuffer(int initialCapacity, ByteOrder byteOrder) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("初始容量不能為負數: " + initialCapacity);
        }
        this.capacity = Math.max(initialCapacity, DEFAULT_CAPACITY);
        this.buffer = new byte[this.capacity];
        this.writeIndex = 0;
        this.readIndex = 0;
        this.byteOrder = byteOrder != null ? byteOrder : ByteOrder.BIG_ENDIAN;
    }

    // ==================== 緩衝區控制方法 ====================

    /**
     * 重置緩衝區以供重複使用
     */
    public ByteArrayBuffer clear() {
        this.writeIndex = 0;
        this.readIndex = 0;
        return this;
    }

    /**
     * 取得當前字節序
     */
    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    /**
     * 設定字節序
     */
    public ByteArrayBuffer setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder != null ? byteOrder : ByteOrder.BIG_ENDIAN;
        return this;
    }

    /**
     * 複製緩衝區
     */
    public ByteArrayBuffer copy() {
        ByteArrayBuffer copy = new ByteArrayBuffer(writeIndex);
        copy.writeBytes(Arrays.copyOf(buffer, writeIndex));
        return copy;
    }

    /**
     * 克隆緩衝區（深拷貝）
     * 創建一個完全獨立的緩衝區副本，包括所有狀態和資料
     */
    @Override
    public ByteArrayBuffer clone() {
        try {
            ByteArrayBuffer cloned = new ByteArrayBuffer(this.capacity, this.byteOrder);

            // 複製緩衝區資料
            System.arraycopy(this.buffer, 0, cloned.buffer, 0, this.capacity);

            // 複製所有狀態
            cloned.writeIndex = this.writeIndex;
            cloned.readIndex = this.readIndex;

            return cloned;
        } catch (Exception e) {
            throw new RuntimeException("克隆緩衝區時發生錯誤: " + e.getMessage(), e);
        }
    }

    // ==================== 容量管理方法 ====================

    /**
     * 確保緩衝區有足夠的容量
     * 
     * @param minCapacity 最小所需容量
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > capacity) {
            int newCapacity = calculateNewCapacity(minCapacity);
            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(buffer, 0, newBuffer, 0, writeIndex);
            buffer = newBuffer;
            capacity = newCapacity;
        }
    }

    /**
     * 計算新的容量大小
     */
    private int calculateNewCapacity(int minCapacity) {
        if (minCapacity > MAX_CAPACITY) {
            throw new OutOfMemoryError("所需容量超過最大限制");
        }

        int newCapacity = capacity;
        while (newCapacity < minCapacity) {
            newCapacity = Math.min(newCapacity * 2, MAX_CAPACITY);
        }
        return newCapacity;
    }

    // ==================== 基本數據類型寫入方法 ====================

    /**
     * 寫入整數 (4 bytes)
     */
    public ByteArrayBuffer writeInt(int value) {
        ensureCapacity(writeIndex + 4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            buffer[writeIndex++] = (byte) (value >>> 24);
            buffer[writeIndex++] = (byte) (value >>> 16);
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) value;
        } else {
            // Little-Endian
            buffer[writeIndex++] = (byte) value;
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) (value >>> 16);
            buffer[writeIndex++] = (byte) (value >>> 24);
        }
        return this;
    }

    /**
     * 寫入長整數 (8 bytes)
     */
    public ByteArrayBuffer writeLong(long value) {
        ensureCapacity(writeIndex + 8);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            buffer[writeIndex++] = (byte) (value >>> 56);
            buffer[writeIndex++] = (byte) (value >>> 48);
            buffer[writeIndex++] = (byte) (value >>> 40);
            buffer[writeIndex++] = (byte) (value >>> 32);
            buffer[writeIndex++] = (byte) (value >>> 24);
            buffer[writeIndex++] = (byte) (value >>> 16);
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) value;
        } else {
            // Little-Endian
            buffer[writeIndex++] = (byte) value;
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) (value >>> 16);
            buffer[writeIndex++] = (byte) (value >>> 24);
            buffer[writeIndex++] = (byte) (value >>> 32);
            buffer[writeIndex++] = (byte) (value >>> 40);
            buffer[writeIndex++] = (byte) (value >>> 48);
            buffer[writeIndex++] = (byte) (value >>> 56);
        }
        return this;
    }

    /**
     * 寫入短整數 (2 bytes)
     */
    public ByteArrayBuffer writeShort(short value) {
        ensureCapacity(writeIndex + 2);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            buffer[writeIndex++] = (byte) (value >>> 8);
            buffer[writeIndex++] = (byte) value;
        } else {
            // Little-Endian
            buffer[writeIndex++] = (byte) value;
            buffer[writeIndex++] = (byte) (value >>> 8);
        }
        return this;
    }

    /**
     * 寫入位元組
     */
    public ByteArrayBuffer writeByte(byte value) {
        ensureCapacity(writeIndex + 1);
        buffer[writeIndex++] = value;
        return this;
    }

    /**
     * 寫入字元值
     */
    public ByteArrayBuffer writeChar(char value) {
        return writeShort((short) value);
    }

    /**
     * 寫入布林值 (1 byte)
     */
    public ByteArrayBuffer writeBool(boolean value) {
        return writeByte((byte) (value ? 1 : 0));
    }

    /**
     * 寫入浮點數 (4 bytes)
     */
    public ByteArrayBuffer writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    /**
     * 寫入雙精度浮點數 (8 bytes)
     */
    public ByteArrayBuffer writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    // ==================== 複合數據類型寫入方法 ====================

    /**
     * 寫入字串 (UTF-8 編碼)
     * 格式: [長度(4 bytes)][字串資料]
     */
    public ByteArrayBuffer writeString(String value) {
        if (value == null) {
            return writeInt(-1);
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeInt(bytes.length);
        return writeBytes(bytes);
    }

    /**
     * 寫入位元組陣列
     */
    public ByteArrayBuffer writeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return this;
        }

        ensureCapacity(writeIndex + bytes.length);
        System.arraycopy(bytes, 0, buffer, writeIndex, bytes.length);
        writeIndex += bytes.length;
        return this;
    }

    /**
     * 寫入 Date 物件
     * 格式: [時間戳記(8 bytes)]
     */
    public ByteArrayBuffer writeDate(Date value) {
        if (value == null) {
            return writeLong(-1L); // 使用 -1 表示 null
        }
        return writeLong(value.getTime());
    }

    /**
     * 寫入 BigInteger 物件
     * 格式: [長度(4 bytes)][位元組資料]
     */
    public ByteArrayBuffer writeBigInteger(BigInteger value) {
        if (value == null) {
            return writeInt(-1);
        }

        byte[] bytes = value.toByteArray();
        writeInt(bytes.length);
        return writeBytes(bytes);
    }

    /**
     * 寫入 BigDecimal 物件
     * 格式: [scale(4 bytes)][BigInteger長度(4 bytes)][BigInteger位元組資料]
     */
    public ByteArrayBuffer writeBigDecimal(BigDecimal value) {
        if (value == null) {
            writeInt(-1); // scale = -1 表示 null
            return writeInt(-1); // length = -1 表示 null
        }

        writeInt(value.scale());
        return writeBigInteger(value.unscaledValue());
    }

    // ==================== JSON 支援方法 ====================

    /**
     * 寫入 JSON 物件（序列化為字串）
     * 格式: [JSON字串長度(4 bytes)][JSON字串資料]
     */
    public ByteArrayBuffer writeJson(Object object) {
        if (object == null) {
            return writeString(null);
        }

        String jsonString = JsonUtil.toJson(object);
        return writeString(jsonString);
    }

    /**
     * 寫入 JSON 字串
     * 格式: [JSON字串長度(4 bytes)][JSON字串資料]
     */
    public ByteArrayBuffer writeJsonString(String jsonString) {
        // 驗證 JSON 格式
        if (jsonString != null && !JsonUtil.isValidJson(jsonString)) {
            throw new IllegalArgumentException("Invalid JSON format: " + jsonString);
        }
        return writeString(jsonString);
    }

    // ==================== 基本數據類型讀取方法 ====================

    /**
     * 讀取整數 (4 bytes)
     */
    public int readInt() {
        checkReadableBytes(4);
        int value;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            value = ((buffer[readIndex] & 0xFF) << 24) |
                    ((buffer[readIndex + 1] & 0xFF) << 16) |
                    ((buffer[readIndex + 2] & 0xFF) << 8) |
                    (buffer[readIndex + 3] & 0xFF);
        } else {
            // Little-Endian
            value = (buffer[readIndex] & 0xFF) |
                    ((buffer[readIndex + 1] & 0xFF) << 8) |
                    ((buffer[readIndex + 2] & 0xFF) << 16) |
                    ((buffer[readIndex + 3] & 0xFF) << 24);
        }
        readIndex += 4;
        return value;
    }

    /**
     * 讀取長整數 (8 bytes)
     */
    public long readLong() {
        checkReadableBytes(8);
        long value;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            value = ((long) (buffer[readIndex] & 0xFF) << 56) |
                    ((long) (buffer[readIndex + 1] & 0xFF) << 48) |
                    ((long) (buffer[readIndex + 2] & 0xFF) << 40) |
                    ((long) (buffer[readIndex + 3] & 0xFF) << 32) |
                    ((long) (buffer[readIndex + 4] & 0xFF) << 24) |
                    ((long) (buffer[readIndex + 5] & 0xFF) << 16) |
                    ((long) (buffer[readIndex + 6] & 0xFF) << 8) |
                    (long) (buffer[readIndex + 7] & 0xFF);
        } else {
            // Little-Endian
            value = (long) (buffer[readIndex] & 0xFF) |
                    ((long) (buffer[readIndex + 1] & 0xFF) << 8) |
                    ((long) (buffer[readIndex + 2] & 0xFF) << 16) |
                    ((long) (buffer[readIndex + 3] & 0xFF) << 24) |
                    ((long) (buffer[readIndex + 4] & 0xFF) << 32) |
                    ((long) (buffer[readIndex + 5] & 0xFF) << 40) |
                    ((long) (buffer[readIndex + 6] & 0xFF) << 48) |
                    ((long) (buffer[readIndex + 7] & 0xFF) << 56);
        }
        readIndex += 8;
        return value;
    }

    /**
     * 讀取短整數 (2 bytes)
     */
    public short readShort() {
        checkReadableBytes(2);
        short value;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            // Big-Endian（網絡字節序）
            value = (short) (((buffer[readIndex] & 0xFF) << 8) |
                    (buffer[readIndex + 1] & 0xFF));
        } else {
            // Little-Endian
            value = (short) ((buffer[readIndex] & 0xFF) |
                    ((buffer[readIndex + 1] & 0xFF) << 8));
        }
        readIndex += 2;
        return value;
    }

    /**
     * 讀取位元組
     */
    public byte readByte() {
        checkReadableBytes(1);
        return buffer[readIndex++];
    }

    /**
     * 讀取字元值
     */
    public char readChar() {
        return (char) readShort();
    }

    /**
     * 讀取布林值
     */
    public boolean readBool() {
        return readByte() != 0;
    }

    /**
     * 讀取浮點數
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * 讀取雙精度浮點數
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    // ==================== 複合數據類型讀取方法 ====================

    /**
     * 讀取字串
     */
    public String readString() {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        if (length == 0) {
            return "";
        }

        checkReadableBytes(length);
        byte[] bytes = new byte[length];
        System.arraycopy(buffer, readIndex, bytes, 0, length);
        readIndex += length;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 讀取位元組陣列
     */
    public byte[] readBytes(int length) {
        checkReadableBytes(length);
        byte[] bytes = new byte[length];
        System.arraycopy(buffer, readIndex, bytes, 0, length);
        readIndex += length;
        return bytes;
    }

    /**
     * 讀取 Date 物件
     */
    public Date readDate() {
        long timestamp = readLong();
        if (timestamp == -1L) {
            return null;
        }
        return new Date(timestamp);
    }

    /**
     * 讀取 BigInteger 物件
     */
    public BigInteger readBigInteger() {
        int length = readInt();
        if (length == -1) {
            return null;
        }
        if (length == 0) {
            return BigInteger.ZERO;
        }

        byte[] bytes = readBytes(length);
        return new BigInteger(bytes);
    }

    /**
     * 讀取 BigDecimal 物件
     */
    public BigDecimal readBigDecimal() {
        int scale = readInt();
        if (scale == -1) {
            readInt(); // 讀取並忽略長度欄位
            return null;
        }

        BigInteger unscaledValue = readBigInteger();
        if (unscaledValue == null) {
            return null;
        }

        return new BigDecimal(unscaledValue, scale);
    }

    // ==================== JSON 支援讀取方法 ====================

    /**
     * 讀取 JSON 字串
     */
    public String readJsonString() {
        return readString();
    }

    /**
     * 讀取並反序列化 JSON 物件
     * 
     * @param clazz 目標類型
     * @return 反序列化後的物件
     */
    public <T> T readJson(Class<T> clazz) {
        String jsonString = readString();
        if (jsonString == null) {
            return null;
        }
        return JsonUtil.fromJson(jsonString, clazz);
    }

    // ==================== 緩衝區狀態查詢方法 ====================

    /**
     * 檢查是否有足夠的可讀位元組
     */
    private void checkReadableBytes(int length) {
        if (readableBytes() < length) {
            throw new IndexOutOfBoundsException(
                    String.format("可讀位元組不足: 需要 %d, 實際 %d", length, readableBytes()));
        }
    }

    /**
     * 取得可讀位元組數
     */
    public int readableBytes() {
        return writeIndex - readIndex;
    }

    /**
     * 取得可寫位元組數
     */
    public int writableBytes() {
        return capacity - writeIndex;
    }

    /**
     * 是否可讀
     */
    public boolean isReadable() {
        return readableBytes() > 0;
    }

    /**
     * 是否可寫
     */
    public boolean isWritable() {
        return writableBytes() > 0;
    }

    /**
     * 取得緩衝區容量
     */
    public int capacity() {
        return capacity;
    }

    // ==================== 索引控制方法 ====================

    /**
     * 取得目前的讀取索引
     */
    public int readerIndex() {
        return readIndex;
    }

    /**
     * 取得目前的寫入索引
     */
    public int writerIndex() {
        return writeIndex;
    }

    /**
     * 設定讀取索引
     */
    public ByteArrayBuffer readerIndex(int index) {
        if (index < 0 || index > writeIndex) {
            throw new IndexOutOfBoundsException("讀取索引超出範圍: " + index);
        }
        this.readIndex = index;
        return this;
    }

    /**
     * 設定寫入索引
     */
    public ByteArrayBuffer writerIndex(int index) {
        if (index < readIndex || index > capacity) {
            throw new IndexOutOfBoundsException("寫入索引超出範圍: " + index);
        }
        this.writeIndex = index;
        return this;
    }

    // ==================== 實用方法 ====================

    /**
     * 轉換為位元組陣列（僅包含有效資料）
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(buffer, 0, writeIndex);
    }

    /**
     * 寫入結構化物件
     * 使用 @MessageTag 註解標記的欄位會按照 order 順序進行序列化
     * 支援繼承關係和泛型類型
     * 
     * @param obj 要序列化的物件
     * @throws IllegalArgumentException 如果物件為 null 或沒有 @MessageTag 欄位
     * @throws RuntimeException         如果序列化過程中發生錯誤
     */
    public ByteArrayBuffer writeStruct(Object obj) {
        try {
            if (obj == null) {
                writeBool(false);
                return this;
            }
            writeBool(true);

            // 寫入實際類型名稱以便反序列化時能正確建立物件
            writeString(obj.getClass().getName());

            return writeStructValue(obj);
        } catch (Throwable e) {
            throw new RuntimeException("寫入結構時發生錯誤: " + e.getMessage(), e);
        }
    }

    /**
     * 寫入結構化物件的值（內部使用）
     */
    private ByteArrayBuffer writeStructValue(Object obj) throws Throwable {
        Class<?> clazz = obj.getClass();

        // 收集所有帶有 @MessageTag 註解的欄位（包括父類別）
        Field[] fields = getAllMessageTagFields(clazz);

        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(obj);
            writeValue(field.getType(), fieldValue);
        }

        return this;
    }

    /**
     * 遞迴寫入值
     */
    private ByteArrayBuffer writeValue(Class<?> clazz, Object value) throws Throwable {
        // null 值處理
        if (value == null) {
            return writeBool(false);
        }
        writeBool(true); // 非 null 標記

        // 基本型別處理
        if (clazz == Boolean.class || clazz == boolean.class) {
            writeBool((Boolean) value);
            return this;
        } else if (clazz == Character.class || clazz == char.class) {
            writeChar((Character) value);
            return this;
        } else if (clazz == Byte.class || clazz == byte.class) {
            writeByte((Byte) value);
            return this;
        } else if (clazz == Short.class || clazz == short.class) {
            writeShort((Short) value);
            return this;
        } else if (clazz == Integer.class || clazz == int.class) {
            return writeInt((Integer) value);
        } else if (clazz == Long.class || clazz == long.class) {
            return writeLong((Long) value);
        } else if (clazz == Float.class || clazz == float.class) {
            return writeFloat((Float) value);
        } else if (clazz == Double.class || clazz == double.class) {
            return writeDouble((Double) value);
        } else if (clazz == BigDecimal.class) {
            return writeBigDecimal((BigDecimal) value);
        } else if (clazz == BigInteger.class) {
            return writeBigInteger((BigInteger) value);
        } else if (clazz == String.class) {
            return writeString((String) value);
        } else if (clazz == Date.class) {
            return writeDate((Date) value);
        }

        // 陣列處理
        if (clazz.isArray()) {
            int len = Array.getLength(value);
            writeInt(len);
            Class<?> componentType = clazz.getComponentType();
            for (int i = 0; i < len; i++) {
                writeValue(componentType, Array.get(value, i));
            }
            return this;
        }

        // 結構物件處理（遞迴序列化）
        return writeStruct(value);
    }

    /**
     * 獲取所有帶有 @MessageTag 註解的欄位（包括父類別），並按 order 排序
     */
    private Field[] getAllMessageTagFields(Class<?> clazz) {
        java.util.List<Field> allFields = new java.util.ArrayList<>();

        // 遞迴收集所有類別（包括父類別）的欄位
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(MessageTag.class)) {
                    allFields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // 按 order 排序
        allFields.sort((f1, f2) -> {
            MessageTag tag1 = f1.getAnnotation(MessageTag.class);
            MessageTag tag2 = f2.getAnnotation(MessageTag.class);
            return Integer.compare(tag1.order(), tag2.order());
        });

        return allFields.toArray(new Field[0]);
    }

    /**
     * 讀取結構化物件
     * 支援繼承關係和泛型類型
     * 
     * @param clazz 期望的類型（可以是父類別或介面）
     * @return 反序列化後的物件
     */
    @SuppressWarnings("unchecked")
    public <T> T readStruct(Class<T> clazz) {
        try {
            // 檢查 null 標記
            if (!readBool()) {
                return null;
            }

            // 讀取實際類型名稱
            String actualClassName = readString();
            if (actualClassName == null) {
                throw new RuntimeException("無法讀取物件類型名稱");
            }

            // 載入實際類型
            Class<?> actualClass;
            try {
                actualClass = Class.forName(actualClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("找不到類別: " + actualClassName, e);
            }

            // 驗證類型相容性
            if (!clazz.isAssignableFrom(actualClass)) {
                throw new RuntimeException(String.format("類型不相容: 期望 %s，實際 %s",
                        clazz.getName(), actualClass.getName()));
            }

            return (T) readStructValue(actualClass);
        } catch (Throwable e) {
            throw new RuntimeException("讀取結構時發生錯誤: " + e.getMessage(), e);
        }
    }

    /**
     * 讀取結構化物件的值（內部使用）
     */
    private Object readStructValue(Class<?> clazz) throws Throwable {
        // 建立物件實例
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 收集所有帶有 @MessageTag 註解的欄位（包括父類別）
        Field[] fields = getAllMessageTagFields(clazz);

        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = readValue(field.getType());
            field.set(instance, fieldValue);
        }

        return instance;
    }

    /**
     * 遞迴讀取值
     */
    private Object readValue(Class<?> clazz) throws Throwable {
        // 檢查 null 標記
        if (!readBool()) {
            return null;
        }

        // 基本型別處理
        if (clazz == Boolean.class || clazz == boolean.class) {
            return readBool();
        } else if (clazz == Character.class || clazz == char.class) {
            return readChar();
        } else if (clazz == Byte.class || clazz == byte.class) {
            return readByte();
        } else if (clazz == Short.class || clazz == short.class) {
            return readShort();
        } else if (clazz == Integer.class || clazz == int.class) {
            return readInt();
        } else if (clazz == Long.class || clazz == long.class) {
            return readLong();
        } else if (clazz == Float.class || clazz == float.class) {
            return readFloat();
        } else if (clazz == Double.class || clazz == double.class) {
            return readDouble();
        } else if (clazz == BigDecimal.class) {
            return readBigDecimal();
        } else if (clazz == BigInteger.class) {
            return readBigInteger();
        } else if (clazz == String.class) {
            return readString();
        } else if (clazz == Date.class) {
            return readDate();
        }

        // 陣列處理
        if (clazz.isArray()) {
            int len = readInt();
            Class<?> componentType = clazz.getComponentType();
            Object array = Array.newInstance(componentType, len);
            for (int i = 0; i < len; i++) {
                Array.set(array, i, readValue(componentType));
            }
            return array;
        }

        // 結構物件處理（遞迴反序列化）
        return readStruct(clazz);
    }

    @Override
    public String toString() {
        return String.format("ByteArrayBuffer(capacity=%d, readable=%d, writable=%d)",
                capacity, readableBytes(), writableBytes());
    }
}
