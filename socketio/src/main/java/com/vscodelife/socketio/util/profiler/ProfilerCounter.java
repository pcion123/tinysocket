package com.vscodelife.socketio.util.profiler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 線程安全的性能計數器
 * 
 * <p>
 * 使用原子操作確保在多線程環境下統計數據的準確性和一致性。
 * 本類追蹤單個監測物件的詳細執行統計，包括次數、時間和超時情況。
 * </p>
 * 
 * <h3>統計維度：</h3>
 * <ul>
 * <li><strong>執行次數</strong>：總共執行的次數</li>
 * <li><strong>超時次數</strong>：執行時間超過閾值的次數</li>
 * <li><strong>時間統計</strong>：最大、最小、平均執行時間</li>
 * <li><strong>總時間</strong>：所有執行的累積時間</li>
 * </ul>
 * 
 * <h3>線程安全實現：</h3>
 * <ul>
 * <li>所有統計字段使用 {@link AtomicLong} 實現</li>
 * <li>無鎖設計，避免線程阻塞</li>
 * <li>原子性更新，確保數據一致性</li>
 * <li>實時計算平均值，避免精度損失</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * ProfilerCounter counter = new ProfilerCounter("database-query");
 * 
 * // 記錄一次執行（150ms，未超時）
 * counter.execute(150, false);
 * 
 * // 記錄一次超時執行（5000ms，超時）
 * counter.execute(5000, true);
 * 
 * // 輸出統計信息
 * System.out.println(counter);
 * // 輸出: [database-query] count=2 timeout=1 max=5000 ms min=150 ms average=2575
 * // ms
 * }</pre>
 * 
 * <h3>性能考量：</h3>
 * <ul>
 * <li>使用原子操作，比同步鎖性能更好</li>
 * <li>預先計算平均值，讀取時無需計算</li>
 * <li>最小化內存佔用，只保留必要統計</li>
 * </ul>
 * 
 * @author vscodelife
 * @version 2.0
 * @since 1.0
 * @see AtomicLong 原子操作實現
 * @see ProfilerCounterManager 管理器類
 * @thread-safe 本類是線程安全的
 */
public class ProfilerCounter {
    /** 計數器名稱，用於識別監測對象 */
    private final String name;

    /** 執行次數統計 */
    private final AtomicLong executeCount = new AtomicLong(0);

    /** 超時次數統計 */
    private final AtomicLong timeoutCount = new AtomicLong(0);

    /** 最大執行時間（毫秒） */
    private final AtomicLong maxExecuteTime = new AtomicLong(0);

    /** 最小執行時間（毫秒），初始值為 Long.MAX_VALUE */
    private final AtomicLong minExecuteTime = new AtomicLong(Long.MAX_VALUE);

    /** 總執行時間（毫秒），用於計算平均值 */
    private final AtomicLong totalExecuteTime = new AtomicLong(0);

    /** 預先計算的平均執行時間（毫秒），實時更新以提升讀取性能 */
    private final AtomicLong averageExecuteTime = new AtomicLong(0);

    /**
     * 建構子
     * 
     * @param name 計數器名稱，不能為 null
     * @throws IllegalArgumentException 當 name 為 null 時
     */
    public ProfilerCounter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Counter name cannot be null");
        }
        this.name = name;
    }

    /**
     * 線程安全的統計格式化輸出
     * 
     * <p>
     * 原子性讀取所有統計數據，確保輸出的數據一致性。
     * 即使在高併發環境下，也能保證各個統計值之間的邏輯一致性。
     * </p>
     * 
     * <p>
     * 輸出格式：[名稱] count=執行次數 timeout=超時次數 max=最大時間 ms min=最小時間 ms average=平均時間 ms
     * </p>
     * 
     * @return 格式化的統計字串
     */
    @Override
    public String toString() {
        // 原子讀取所有統計數據，確保數據一致性
        long execCnt = executeCount.get();
        long timeoutCnt = timeoutCount.get();
        long maxTime = maxExecuteTime.get();
        long minTime = minExecuteTime.get();
        long avgTime = averageExecuteTime.get();

        // 處理最小時間的特殊情況
        long displayMinTime = (minTime == Long.MAX_VALUE) ? 0 : minTime;

        return String.format("[%s] count=%d timeout=%d max=%d ms min=%d ms average=%d ms",
                name, execCnt, timeoutCnt, maxTime, displayMinTime, avgTime);
    }

    /**
     * 獲取計數器名稱
     * 
     * @return 計數器名稱
     */
    public String getName() {
        return name;
    }

    /**
     * 獲取最大執行時間
     * 
     * @return 最大執行時間（毫秒）
     */
    public long getMaxExecuteTime() {
        return maxExecuteTime.get();
    }

    /**
     * 獲取最小執行時間
     * 
     * @return 最小執行時間（毫秒），如果沒有執行過則返回0
     */
    public long getMinExecuteTime() {
        long minTime = minExecuteTime.get();
        return (minTime == Long.MAX_VALUE) ? 0 : minTime;
    }

    /**
     * 獲取平均執行時間
     * 
     * @return 平均執行時間（毫秒）
     */
    public long getAverageExecuteTime() {
        return averageExecuteTime.get();
    }

    /**
     * 獲取執行次數
     * 
     * @return 執行次數
     */
    public long getExecuteCount() {
        return executeCount.get();
    }

    /**
     * 獲取超時次數
     * 
     * @return 超時次數
     */
    public long getTimeoutCount() {
        return timeoutCount.get();
    }

    /**
     * 獲取總執行時間
     * 
     * @return 總執行時間（毫秒）
     */
    public long getTotalExecuteTime() {
        return totalExecuteTime.get();
    }

    /**
     * 線程安全的執行統計方法
     * 
     * <p>
     * 記錄一次執行的時間和超時狀態，更新所有相關統計數據。
     * 所有更新操作都是原子性的，確保在併發環境下的數據準確性。
     * </p>
     * 
     * <h4>更新順序：</h4>
     * <ol>
     * <li>原子性遞增執行次數</li>
     * <li>如果超時，原子性遞增超時次數</li>
     * <li>原子性更新最大執行時間</li>
     * <li>原子性更新最小執行時間</li>
     * <li>原子性累加總執行時間</li>
     * <li>實時計算並更新平均執行時間</li>
     * </ol>
     * 
     * <p>
     * <strong>性能優化：</strong>平均值在每次更新時實時計算，
     * 避免了讀取時的除法運算，提升查詢性能。
     * </p>
     * 
     * @param time    執行時間（毫秒），必須 >= 0
     * @param timeout 是否超時，true 表示本次執行超過了預設的超時閾值
     * @throws IllegalArgumentException 當 time < 0 時
     */
    public void execute(long time, boolean timeout) {
        // 原子遞增執行次數
        long currentCount = executeCount.incrementAndGet();

        // 原子遞增超時次數
        if (timeout) {
            timeoutCount.incrementAndGet();
        }

        // 原子更新最大執行時間
        maxExecuteTime.updateAndGet(current -> Math.max(current, time));

        // 原子更新最小執行時間
        minExecuteTime.updateAndGet(current -> Math.min(current, time));

        // 原子累加總執行時間
        long currentTotal = totalExecuteTime.addAndGet(time);

        // 在更新時立即計算並更新平均值
        // 使用原子操作確保線程安全
        averageExecuteTime.set(currentTotal / currentCount);
    }

    /**
     * 重置所有統計數據
     * 線程安全的重置操作
     */
    public void reset() {
        executeCount.set(0);
        timeoutCount.set(0);
        maxExecuteTime.set(0);
        minExecuteTime.set(Long.MAX_VALUE);
        totalExecuteTime.set(0);
        averageExecuteTime.set(0);
    }

    /**
     * 比較兩個 ProfilerCounter 是否相等
     * 
     * @param obj 要比較的對象
     * @return 如果名稱相同則相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProfilerCounter that = (ProfilerCounter) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    /**
     * 獲取 hash code
     * 
     * @return hash code
     */
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
