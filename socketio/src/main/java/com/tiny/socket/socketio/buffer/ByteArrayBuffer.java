package com.tiny.socket.socketio.buffer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tiny.socket.socketio.annotation.Member;
import com.tiny.socket.socketio.util.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

public class ByteArrayBuffer {
    private static Logger logger = LoggerFactory.getLogger(ByteArrayBuffer.class);

    private final static boolean BIGENDIAN = true;
    private final static int BUFFERSIZE = 10 * 1024;

    private ByteBuf buffer;

    public ByteArrayBuffer() {
        this(BUFFERSIZE);
    }

    public ByteArrayBuffer(int capacity) {
        this(PooledByteBufAllocator.DEFAULT.buffer(capacity), null);
    }

    public ByteArrayBuffer(byte[] buffer) {
        this(PooledByteBufAllocator.DEFAULT.buffer(buffer.length), buffer);
    }

    public ByteArrayBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    public ByteArrayBuffer(ByteBuf buffer, byte[] src) {
        this.buffer = buffer;
        if (src != null)
            this.buffer.writeBytes(src);
    }

    public ByteArrayBuffer clone() {
        return new ByteArrayBuffer(copyToByteBuf());
    }

    public boolean release() {
        return (buffer != null && buffer.refCnt() > 0) ? ReferenceCountUtil.release(buffer) : false;
    }

    public int refCnt() {
        return buffer != null ? buffer.refCnt() : -1;
    }

    public int getCapacity() {
        return buffer.capacity();
    }

    public int getSpace() {
        return buffer.writableBytes();
    }

    public int getAvailable() {
        return buffer.readableBytes();
    }

    public ByteArrayBuffer writeBuffer(byte[] value) {
        if (value == null) {

        } else {
            buffer.writeBytes(value);
        }
        return this;
    }

    public ByteArrayBuffer writeBuffer(ByteArrayBuffer value) {
        if (value == null) {

        } else {
            buffer.writeBytes(value.copyToByteArray());
        }
        return this;
    }

    public ByteArrayBuffer writeByteArray(byte[] value) {
        if (value == null) {
            writeInt(0);
        } else {
            writeInt(value.length);
            buffer.writeBytes(value);
        }
        return this;
    }

    public byte[] readByteArray() {
        int len = readInt();
        byte[] tmp = null;
        if (len > 0) {
            tmp = new byte[len];
            buffer.readBytes(tmp);
        }
        return tmp;
    }

    public ByteArrayBuffer writeByte(byte value) {
        buffer.writeByte(value);
        return this;
    }

    public byte readByte() {
        return buffer.readByte();
    }

    public ByteArrayBuffer writeShort(short value) {
        if (BIGENDIAN) {
            buffer.writeShort(value);
        } else {
            buffer.writeShortLE(value);
        }
        return this;
    }

    public short readShort() {
        return BIGENDIAN ? buffer.readShort() : buffer.readShortLE();
    }

    public ByteArrayBuffer writeInt(int value) {
        if (BIGENDIAN) {
            buffer.writeInt(value);
        } else {
            buffer.writeIntLE(value);
        }
        return this;
    }

    public int readInt() {
        return BIGENDIAN ? buffer.readInt() : buffer.readIntLE();
    }

    public ByteArrayBuffer writeLong(long value) {
        if (BIGENDIAN) {
            buffer.writeLong(value);
        } else {
            buffer.writeLongLE(value);
        }
        return this;
    }

    public long readLong() {
        return BIGENDIAN ? buffer.readLong() : buffer.readLongLE();
    }

    public ByteArrayBuffer writeFloat(float value) {
        if (BIGENDIAN) {
            buffer.writeFloat(value);
        } else {
            buffer.writeFloatLE(value);
        }
        return this;
    }

    public float readFloat() {
        return BIGENDIAN ? buffer.readFloat() : buffer.readFloatLE();
    }

    public ByteArrayBuffer writeDouble(double value) {
        if (BIGENDIAN) {
            buffer.writeDouble(value);
        } else {
            buffer.writeDoubleLE(value);
        }
        return this;
    }

    public double readDouble() {
        return BIGENDIAN ? buffer.readDouble() : buffer.readDoubleLE();
    }

    public ByteArrayBuffer writeChar(char value) {
        buffer.writeChar(value);
        return this;
    }

    public char readChar() {
        return buffer.readChar();
    }

    public ByteArrayBuffer writeBool(boolean value) {
        buffer.writeBoolean(value);
        return this;
    }

    public boolean readBool() {
        return buffer.readBoolean();
    }

    public ByteArrayBuffer writeString(String value) {
        if (value == null || "".equals(value)) {
            return writeByteArray(null);
        } else {
            byte[] tmp = value.getBytes(Charset.forName("UTF-8"));
            return writeByteArray(tmp);
        }
    }

    public String readString() {
        try {
            byte[] tmp = readByteArray();
            return tmp != null ? new String(tmp, "UTF-8") : "";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public ByteArrayBuffer writeDate(Date value) {
        return writeLong(value.getTime());
    }

    public Date readDate() {
        return new Date(readLong());
    }

    @SuppressWarnings("rawtypes")
    private Field[] sortMember(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class tmpClazz = clazz;
        while (tmpClazz != null) {
            fields.addAll(Arrays.asList(tmpClazz.getDeclaredFields()));
            tmpClazz = tmpClazz.getSuperclass();
        }
        return sortMember(fields.stream().toArray(Field[]::new));
    }

    private Field[] sortMember(Field[] fields) {
        List<Field> list = Arrays.asList(fields);
        return list.stream().filter(n -> n.isAnnotationPresent(Member.class)).sorted((a, b) -> {
            Member m1 = a.getAnnotation(Member.class);
            Member m2 = b.getAnnotation(Member.class);
            if (m1 != null && m2 != null) {
                return m1.order() - m2.order();
            } else if (m1 != null && m2 == null) {
                return -1;
            } else if (m1 == null && m2 != null) {
                return 1;
            }
            return a.getName().compareTo(b.getName());
        }).toArray(Field[]::new);
    }

    private ByteArrayBuffer writeValue(Class<?> clazz, Object value) throws Throwable {
        if (clazz == Boolean.class || clazz == boolean.class)
            return writeBool((boolean) value);
        else if (clazz == Character.class || clazz == char.class)
            return writeChar((char) value);
        else if (clazz == Byte.class || clazz == byte.class)
            return writeByte((byte) value);
        else if (clazz == Short.class || clazz == short.class)
            return writeShort((short) value);
        else if (clazz == Integer.class || clazz == int.class)
            return writeInt((int) value);
        else if (clazz == Long.class || clazz == long.class)
            return writeLong((long) value);
        else if (clazz == Float.class || clazz == float.class)
            return writeFloat((float) value);
        else if (clazz == Double.class || clazz == double.class)
            return writeDouble((double) value);
        else if (clazz == String.class)
            return writeString((String) value);
        else if (clazz == Date.class)
            return writeDate((Date) value);
        Field[] fields = sortMember(clazz);
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType().isArray()) {
                Member member = field.getAnnotation(Member.class);
                int len = member.length();
                // int len = Array.getLength(field.get(value));
                for (int i = 0; i < len; i++) {
                    writeValue(field.getType().getComponentType(), Array.get(field.get(value), i));
                }
            } else {
                writeValue(field.getType(), field.get(value));
            }
        }
        return this;
    }

    @SuppressWarnings("deprecation")
    private Object readValue(Class<?> clazz) throws Throwable {
        if (clazz == null)
            return null;

        if (clazz == Boolean.class || clazz == boolean.class)
            return new Boolean(readBool());
        else if (clazz == Character.class || clazz == char.class)
            return new Character(readChar());
        else if (clazz == Byte.class || clazz == byte.class)
            return new Byte(readByte());
        else if (clazz == Short.class || clazz == short.class)
            return new Short(readShort());
        else if (clazz == Integer.class || clazz == int.class)
            return new Integer(readInt());
        else if (clazz == Long.class || clazz == long.class)
            return new Long(readLong());
        else if (clazz == Float.class || clazz == float.class)
            return new Float(readFloat());
        else if (clazz == Double.class || clazz == double.class)
            return new Double(readDouble());
        else if (clazz == String.class)
            return readString();
        else if (clazz == Date.class)
            return readDate();
        Object value = clazz.newInstance();
        Field[] fields = sortMember(clazz);
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> fieldClazz = field.getType();
            if (field.getType().isArray()) {
                Member member = field.getAnnotation(Member.class);
                Class<?> arrayClazz = field.getType().getComponentType();
                int len = member.length();
                Object array = Array.newInstance(arrayClazz, len);
                field.set(value, array);
                for (int i = 0; i < len; i++) {
                    Array.set(array, i, readValue(arrayClazz));
                }
            } else {
                field.set(value, readValue(fieldClazz));
            }
        }
        return value;
    }

    public ByteArrayBuffer writeStruct(Class<?> clazz, Object value) {
        try {
            return value != null ? writeValue(clazz, value) : this;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        return this;
    }

    public <T> T readStruct(Class<T> clazz) {
        try {
            return clazz != null ? clazz.cast(readValue(clazz)) : null;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public ByteArrayBuffer writeBufToJson(Object value) {
        return writeString(JsonUtil.obj2JsonStr(value));
    }

    public <T> T readBufToJson(Class<T> clazz) {
        try {
            return clazz != null ? clazz.cast(JsonUtil.toObject(readString(), clazz)) : null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public void compress() {
        // TODO:壓縮
    }

    public void decompress() {
        // TODO:解壓縮
    }

    public void encode(byte code) {
        if (code == 0)
            return;

        int len = buffer.readableBytes();
        byte[] tmp = new byte[len];
        buffer.readBytes(tmp);
        for (int i = 0; i < len; i++)
            tmp[i] ^= code;
        buffer.writeBytes(tmp);
    }

    public void decode(byte code) {
        if (code == 0)
            return;

        int len = buffer.readableBytes();
        byte[] tmp = new byte[len];
        buffer.readBytes(tmp);
        for (int i = 0; i < len; i++)
            tmp[i] ^= code;
        buffer.writeBytes(tmp);
    }

    public byte[] copyToByteArray() {
        ByteBuf buf = buffer.copy(buffer.readerIndex(), buffer.readableBytes());
        byte[] tmp = new byte[buf.readableBytes()];
        buf.readBytes(tmp);
        ReferenceCountUtil.release(buf);
        return tmp;
    }

    public ByteBuf copyToByteBuf() {
        return buffer.copy(buffer.readerIndex(), buffer.readableBytes());
    }
}
