package com.vscodelife.socketio.util;

/**
 * Snowflake ID 生成器工具類
 * 
 * 提供兩種使用方式：
 * 1. 靜態方法（推薦）：全局單例，確保ID唯一性
 * 2. 實例方法：可創建多個生成器，適用於多數據中心場景
 */
public final class SnowflakeUtil {

    // ==================== 常數定義 ====================

    /** 開始時間戳（2020-01-01 00:00:00 UTC） */
    private static final long START_TIMESTAMP = 1577836800000L;

    /** 各部分的位數 */
    private static final long SEQUENCE_BITS = 12L; // 序列號位數
    private static final long WORKER_ID_BITS = 10L; // 工作機器ID位數
    private static final long TIMESTAMP_BITS = 41L; // 時間戳位數

    /** 最大值 */
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    /** 位移量 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    // ==================== 靜態實例變數（全局單例） ====================

    /** 全局默認生成器 */
    private static volatile SnowflakeGenerator defaultGenerator = new SnowflakeGenerator(0L);

    // 私有建構函數，防止實例化
    private SnowflakeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== 靜態方法（推薦使用） ====================

    /**
     * 設定工作機器ID（全局生成器）
     * 
     * @param workerId 工作機器ID (0-1023)
     * @throws IllegalArgumentException 如果 workerId 超出範圍
     */
    public static void setWorkerId(long workerId) {
        validateWorkerId(workerId);
        defaultGenerator = new SnowflakeGenerator(workerId);
    }

    /**
     * 獲取當前工作機器ID（全局生成器）
     * 
     * @return 當前工作機器ID
     */
    public static long getWorkerId() {
        return defaultGenerator.getWorkerId();
    }

    /**
     * 生成下一個唯一ID（全局生成器）
     * 
     * @return 64位唯一ID
     * @throws RuntimeException 如果時鐘回撥
     */
    public static long nextId() {
        return defaultGenerator.nextId();
    }

    /**
     * 批次生成多個唯一ID（全局生成器）
     * 
     * @param count 要生成的ID數量
     * @return ID陣列
     * @throws IllegalArgumentException 如果 count 小於等於 0
     */
    public static long[] nextIds(int count) {
        return defaultGenerator.nextIds(count);
    }

    /**
     * 解析ID，獲取其組成部分
     * 
     * @param id 要解析的ID
     * @return 包含時間戳、工作機器ID、序列號的資訊
     */
    public static IdInfo parseId(long id) {
        return SnowflakeGenerator.parseId(id);
    }

    /**
     * 獲取當前序列號（全局生成器）
     * 
     * @return 當前序列號
     */
    public static long getCurrentSequence() {
        return defaultGenerator.getCurrentSequence();
    }

    /**
     * 重置序列號（僅用於測試）
     */
    public static void resetSequence() {
        defaultGenerator.resetSequence();
    }

    // ==================== 實例化方法 ====================

    /**
     * 創建新的 Snowflake 生成器實例
     * 
     * @param workerId 工作機器ID (0-1023)
     * @return Snowflake 生成器實例
     * @throws IllegalArgumentException 如果 workerId 超出範圍
     */
    public static SnowflakeGenerator createGenerator(long workerId) {
        return new SnowflakeGenerator(workerId);
    }

    // ==================== 工具方法 ====================

    /**
     * 驗證工作機器ID
     * 
     * @param workerId 工作機器ID
     * @throws IllegalArgumentException 如果超出範圍
     */
    private static void validateWorkerId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("WorkerId must be between 0 and %d, but got %d", MAX_WORKER_ID, workerId));
        }
    }

    // ==================== Snowflake 生成器類別 ====================

    /**
     * Snowflake ID 生成器實例
     * 每個實例維護自己的狀態，支援多實例併發使用
     */
    public static class SnowflakeGenerator {

        /** 工作機器ID */
        private final long workerId;

        /** 序列號 */
        private long sequence = 0L;

        /** 上次生成ID的時間戳 */
        private long lastTimestamp = -1L;

        /** 同步鎖 */
        private final Object lock = new Object();

        /**
         * 創建 Snowflake 生成器
         * 
         * @param workerId 工作機器ID (0-1023)
         * @throws IllegalArgumentException 如果 workerId 超出範圍
         */
        public SnowflakeGenerator(long workerId) {
            validateWorkerId(workerId);
            this.workerId = workerId;
        }

        /**
         * 獲取工作機器ID
         * 
         * @return 工作機器ID
         */
        public long getWorkerId() {
            return workerId;
        }

        /**
         * 生成下一個唯一ID
         * 
         * @return 64位唯一ID
         * @throws RuntimeException 如果時鐘回撥
         */
        public long nextId() {
            synchronized (lock) {
                long timestamp = getCurrentTimestamp();

                // 檢查時鐘回撥
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException(
                            String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                                    lastTimestamp - timestamp));
                }

                // 同一毫秒內，序列號自增
                if (timestamp == lastTimestamp) {
                    sequence = (sequence + 1) & MAX_SEQUENCE;
                    // 序列號用盡，等待下一毫秒
                    if (sequence == 0) {
                        timestamp = waitForNextMillis(lastTimestamp);
                    }
                } else {
                    // 不同毫秒，序列號重置
                    sequence = 0L;
                }

                lastTimestamp = timestamp;

                // 組合ID：符號位(1) + 時間戳(41) + 工作機器ID(10) + 序列號(12) = 64位
                return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
                        (workerId << WORKER_ID_SHIFT) |
                        sequence;
            }
        }

        /**
         * 批次生成多個唯一ID
         * 
         * @param count 要生成的ID數量
         * @return ID陣列
         * @throws IllegalArgumentException 如果 count 小於等於 0
         */
        public long[] nextIds(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Count must be greater than 0");
            }

            long[] ids = new long[count];
            for (int i = 0; i < count; i++) {
                ids[i] = nextId();
            }
            return ids;
        }

        /**
         * 獲取當前序列號
         * 
         * @return 當前序列號
         */
        public long getCurrentSequence() {
            return sequence;
        }

        /**
         * 重置序列號（僅用於測試）
         */
        public void resetSequence() {
            synchronized (lock) {
                sequence = 0L;
                lastTimestamp = -1L;
            }
        }

        /**
         * 獲取當前時間戳
         * 
         * @return 當前時間戳（毫秒）
         */
        private long getCurrentTimestamp() {
            return System.currentTimeMillis();
        }

        /**
         * 等待下一毫秒
         * 
         * @param lastTimestamp 上次時間戳
         * @return 下一毫秒時間戳
         */
        private long waitForNextMillis(long lastTimestamp) {
            long timestamp = getCurrentTimestamp();
            while (timestamp <= lastTimestamp) {
                timestamp = getCurrentTimestamp();
            }
            return timestamp;
        }

        /**
         * 解析ID，獲取其組成部分
         * 
         * @param id 要解析的ID
         * @return 包含時間戳、工作機器ID、序列號的資訊
         */
        public static IdInfo parseId(long id) {
            long timestamp = ((id >> TIMESTAMP_SHIFT) & ((1L << TIMESTAMP_BITS) - 1)) + START_TIMESTAMP;
            long parsedWorkerId = (id >> WORKER_ID_SHIFT) & ((1L << WORKER_ID_BITS) - 1);
            long parsedSequence = id & ((1L << SEQUENCE_BITS) - 1);

            return new IdInfo(timestamp, parsedWorkerId, parsedSequence);
        }
    }

    // ==================== 內部類別 ====================

    /**
     * ID 資訊類別
     */
    public static class IdInfo {
        private final long timestamp;
        private final long workerId;
        private final long sequence;

        public IdInfo(long timestamp, long workerId, long sequence) {
            this.timestamp = timestamp;
            this.workerId = workerId;
            this.sequence = sequence;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getWorkerId() {
            return workerId;
        }

        public long getSequence() {
            return sequence;
        }

        @Override
        public String toString() {
            return String.format("IdInfo{timestamp=%d (%s), workerId=%d, sequence=%d}",
                    timestamp, new java.util.Date(timestamp), workerId, sequence);
        }
    }

    // ==================== 測試方法 ====================

    /**
     * 測試 main 方法
     * 
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        System.out.println("=== Snowflake ID 算法測試 ===");
        System.out.println("提供兩種使用方式：");
        System.out.println("1. 靜態方法（全局單例）");
        System.out.println("2. 實例方法（多實例）");
        System.out.println();

        // // 測試靜態方法
        // testStaticMethods();

        // System.out.println("\n" + "=".repeat(50) + "\n");

        // // 測試實例方法
        // testInstanceMethods();

        // System.out.println("\n" + "=".repeat(50) + "\n");

        // // 測試併發安全性
        // testConcurrencySafety();

        // 744215615942049792 逆推 ID 組成部分
        long id = 744215615942049792L;
        IdInfo info = SnowflakeUtil.parseId(id);
        System.out.println("逆推 ID 組成部分:");
        System.out.printf("ID: %d%n", id);
        System.out.printf("  時間戳: %d%n", info.getTimestamp());
        System.out.printf("  工作機器ID: %d%n", info.getWorkerId());
        System.out.printf("  序列號: %d%n", info.getSequence());
    }

    /**
     * 測試靜態方法（推薦使用方式）
     */
    private static void testStaticMethods() {
        System.out.println("=== 靜態方法測試（全局單例） ===");

        // 設定全局工作機器ID
        SnowflakeUtil.setWorkerId(100);
        System.out.println("設定全局工作機器ID: " + SnowflakeUtil.getWorkerId());

        // 生成ID
        System.out.println("\n生成ID（靜態方法）:");
        for (int i = 0; i < 3; i++) {
            long id = SnowflakeUtil.nextId();
            IdInfo info = SnowflakeUtil.parseId(id);
            System.out.printf("ID: %d, WorkerId: %d, Sequence: %d%n",
                    id, info.getWorkerId(), info.getSequence());
        }

        System.out.println("\n=== 靜態方法測試完成 ===");
    }

    /**
     * 測試實例方法（多實例場景）
     */
    private static void testInstanceMethods() {
        System.out.println("=== 實例方法測試（多實例） ===");

        // 創建多個生成器實例
        SnowflakeGenerator generator1 = SnowflakeUtil.createGenerator(1);
        SnowflakeGenerator generator2 = SnowflakeUtil.createGenerator(2);
        SnowflakeGenerator generator3 = SnowflakeUtil.createGenerator(3);

        System.out.println("創建了3個生成器實例:");
        System.out.printf("Generator1 WorkerId: %d%n", generator1.getWorkerId());
        System.out.printf("Generator2 WorkerId: %d%n", generator2.getWorkerId());
        System.out.printf("Generator3 WorkerId: %d%n", generator3.getWorkerId());

        System.out.println("\n各自生成ID:");
        for (int i = 0; i < 3; i++) {
            long id1 = generator1.nextId();
            long id2 = generator2.nextId();
            long id3 = generator3.nextId();

            IdInfo info1 = SnowflakeGenerator.parseId(id1);
            IdInfo info2 = SnowflakeGenerator.parseId(id2);
            IdInfo info3 = SnowflakeGenerator.parseId(id3);

            System.out.printf("Round %d:%n", i + 1);
            System.out.printf("  Gen1: %d (WorkerId:%d, Seq:%d)%n", id1, info1.getWorkerId(), info1.getSequence());
            System.out.printf("  Gen2: %d (WorkerId:%d, Seq:%d)%n", id2, info2.getWorkerId(), info2.getSequence());
            System.out.printf("  Gen3: %d (WorkerId:%d, Seq:%d)%n", id3, info3.getWorkerId(), info3.getSequence());
            System.out.println();
        }

        System.out.println("=== 實例方法測試完成 ===");
    }

    /**
     * 測試併發安全性
     */
    private static void testConcurrencySafety() {
        System.out.println("=== 併發安全性測試 ===");

        // 測試1：靜態方法併發安全性
        System.out.println("測試1：靜態方法併發安全性");
        testStaticConcurrency();

        System.out.println("\n" + "-".repeat(30) + "\n");

        // 測試2：實例方法併發安全性
        System.out.println("測試2：實例方法併發安全性");
        testInstanceConcurrency();

        System.out.println("\n=== 併發安全性測試完成 ===");
    }

    /**
     * 測試靜態方法的併發安全性
     */
    private static void testStaticConcurrency() {
        SnowflakeUtil.setWorkerId(999);
        SnowflakeUtil.resetSequence();

        int threadCount = 5;
        int idsPerThread = 1000;
        java.util.Set<Long> allIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        java.util.List<Thread> threads = new java.util.ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < idsPerThread; i++) {
                    long id = SnowflakeUtil.nextId(); // 使用靜態方法
                    allIds.add(id);
                }
                System.out.printf("靜態方法 - 線程 %d 完成%n", threadId);
            });
            threads.add(thread);
            thread.start();
        }

        // 等待所有線程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();

        int expectedCount = threadCount * idsPerThread;
        int actualCount = allIds.size();

        System.out.printf("靜態方法併發結果：預期 %d，實際 %d，重複 %d，耗時 %dms%n",
                expectedCount, actualCount, expectedCount - actualCount, endTime - startTime);
    }

    /**
     * 測試實例方法的併發安全性
     */
    private static void testInstanceConcurrency() {
        int threadCount = 5;
        int idsPerThread = 1000;
        java.util.Set<Long> allIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        java.util.List<Thread> threads = new java.util.ArrayList<>();

        // 創建一個共享的生成器實例
        SnowflakeGenerator sharedGenerator = SnowflakeUtil.createGenerator(888);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < idsPerThread; i++) {
                    long id = sharedGenerator.nextId(); // 使用實例方法
                    allIds.add(id);
                }
                System.out.printf("實例方法 - 線程 %d 完成%n", threadId);
            });
            threads.add(thread);
            thread.start();
        }

        // 等待所有線程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();

        int expectedCount = threadCount * idsPerThread;
        int actualCount = allIds.size();

        System.out.printf("實例方法併發結果：預期 %d，實際 %d，重複 %d，耗時 %dms%n",
                expectedCount, actualCount, expectedCount - actualCount, endTime - startTime);
    }

}
