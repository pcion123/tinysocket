package com.vscodelife.socketio.util.profiler;

import java.util.concurrent.TimeUnit;

/**
 * ProfilerUtil 配置管理類
 * 
 * <p>
 * 集中管理所有性能監測相關的配置參數，支援靈活的參數調整和多環境配置。
 * 本類採用建造者模式和靜態工廠方法，提供便捷的配置構建方式。
 * </p>
 * 
 * <h3>配置分類：</h3>
 * <ul>
 * <li><strong>清理機制配置</strong>：數據保留時間、清理間隔、計數器限制等</li>
 * <li><strong>超時配置</strong>：預設超時時間、超時日誌開關等</li>
 * <li><strong>監控配置</strong>：工作節點ID、詳細日誌、監控開關等</li>
 * <li><strong>性能配置</strong>：高性能模式、批次處理大小等</li>
 * </ul>
 * 
 * <h3>預設環境配置：</h3>
 * <ul>
 * <li><strong>開發環境</strong>：頻繁清理、詳細日誌、較短超時</li>
 * <li><strong>生產環境</strong>：較長保留、高性能模式、較少日誌</li>
 * <li><strong>測試環境</strong>：快速清理、完整監控、短超時</li>
 * <li><strong>高負載環境</strong>：優化性能、減少開銷、大容量</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <h4>使用預設環境配置：</h4>
 * 
 * <pre>{@code
 * // 應用生產環境配置
 * ProfilerConfig prodConfig = ProfilerConfig.forProduction();
 * ProfilerUtil.setConfig(prodConfig);
 * 
 * // 應用開發環境配置
 * ProfilerConfig devConfig = ProfilerConfig.forDevelopment();
 * }</pre>
 * 
 * <h4>使用建造者模式：</h4>
 * 
 * <pre>{@code
 * ProfilerConfig customConfig = new ProfilerConfig()
 *         .withDataRetention(2, TimeUnit.HOURS)
 *         .withCleanupInterval(30, TimeUnit.MINUTES)
 *         .withMaxCounters(500)
 *         .withDefaultTimeout(5, TimeUnit.SECONDS)
 *         .enableVerboseLogging()
 *         .enableHighPerformanceMode();
 * 
 * ProfilerUtil.setConfig(customConfig);
 * }</pre>
 * 
 * <h4>複製和修改現有配置：</h4>
 * 
 * <pre>{@code
 * ProfilerConfig baseConfig = ProfilerConfig.forProduction();
 * ProfilerConfig modifiedConfig = new ProfilerConfig(baseConfig)
 *         .withMaxCounters(10000)
 *         .enableVerboseLogging();
 * }</pre>
 * 
 * <h3>配置驗證：</h3>
 * <p>
 * 所有配置在應用前都會經過 {@link #validate()} 方法驗證，
 * 確保參數的有效性和邏輯一致性。
 * </p>
 * 
 * @author vscodelife
 * @version 2.0
 * @since 1.0
 * @see ProfilerUtil#setConfig(ProfilerConfig) 配置應用方法
 * @see #validate() 配置驗證邏輯
 */
public class ProfilerConfig {

    // ==================== 清理機制配置 ====================

    /** 數據保留時間(毫秒) - 預設24小時，超過此時間的計數器將被清理 */
    private long dataRetentionTimeMs = TimeUnit.HOURS.toMillis(24);

    /** 最大計數器數量 - 預設1000個，超過此數量會觸發最舊數據清理 */
    private int maxCountersSize = 1000;

    /** 清理間隔時間(毫秒) - 預設30分鐘，自動清理機制的執行間隔 */
    private long cleanupIntervalMs = TimeUnit.MINUTES.toMillis(30);

    /** 孤立快取清理時間(毫秒) - 預設1小時，超過此時間的快取項目將被清理 */
    private long orphanedCacheTimeMs = TimeUnit.HOURS.toMillis(1);

    // ==================== 超時配置 ====================

    /** 預設超時時間(毫秒) - 預設5秒，執行時間超過此值會被標記為超時 */
    private long defaultTimeoutMs = TimeUnit.SECONDS.toMillis(5);

    /** 超時日誌開關 - 預設啟用，控制是否記錄超時操作的日誌 */
    private boolean timeoutLoggingEnabled = true;

    /** 超時統計開關 - 預設啟用，控制是否統計超時次數 */
    private boolean timeoutCountingEnabled = true;

    // ==================== 監控配置 ====================

    /** ID生成器工作節點ID - 預設1，用於雪花算法生成唯一ID */
    private long workerId = 1;

    /** 是否啟用詳細日誌 - 預設關閉，啟用後會輸出更多調試資訊 */
    private boolean verboseLoggingEnabled = false;

    /** 監控開關 - 預設啟用，控制整個性能監測功能是否生效 */
    private boolean monitoringEnabled = true;

    // ==================== 性能配置 ====================

    /** 是否使用高性能模式 - 預設關閉（優先穩定性），啟用後會優化性能但可能減少功能 */
    private boolean highPerformanceMode = false;

    /** 批次處理大小 - 預設100，批量操作時的單次處理數量 */
    private int batchSize = 100;

    /** 統計輸出格式模板 */
    private String statisticsFormat = "[%s] count=%d timeout=%d max=%d ms min=%d ms average=%d ms";

    // ==================== 建構函數 ====================

    /**
     * 預設建構函數 - 使用預設配置
     */
    public ProfilerConfig() {
        // 使用預設值
    }

    /**
     * 複製建構函數
     */
    public ProfilerConfig(ProfilerConfig other) {
        this.dataRetentionTimeMs = other.dataRetentionTimeMs;
        this.maxCountersSize = other.maxCountersSize;
        this.cleanupIntervalMs = other.cleanupIntervalMs;
        this.orphanedCacheTimeMs = other.orphanedCacheTimeMs;
        this.defaultTimeoutMs = other.defaultTimeoutMs;
        this.timeoutLoggingEnabled = other.timeoutLoggingEnabled;
        this.timeoutCountingEnabled = other.timeoutCountingEnabled;
        this.workerId = other.workerId;
        this.verboseLoggingEnabled = other.verboseLoggingEnabled;
        this.monitoringEnabled = other.monitoringEnabled;
        this.highPerformanceMode = other.highPerformanceMode;
        this.batchSize = other.batchSize;
        this.statisticsFormat = other.statisticsFormat;
    }

    // ==================== 靜態工廠方法 ====================

    /**
     * 開發環境配置 - 較頻繁的清理，詳細日誌
     */
    public static ProfilerConfig forDevelopment() {
        ProfilerConfig config = new ProfilerConfig();
        config.setDataRetentionTimeMs(TimeUnit.MINUTES.toMillis(30)); // 30分鐘保留
        config.setCleanupIntervalMs(TimeUnit.MINUTES.toMillis(5)); // 5分鐘清理一次
        config.setMaxCountersSize(100); // 較小的計數器限制
        config.setVerboseLoggingEnabled(true); // 啟用詳細日誌
        config.setDefaultTimeoutMs(TimeUnit.SECONDS.toMillis(2)); // 較短超時時間
        return config;
    }

    /**
     * 生產環境配置 - 較長保留期，較少日誌
     */
    public static ProfilerConfig forProduction() {
        ProfilerConfig config = new ProfilerConfig();
        config.setDataRetentionTimeMs(TimeUnit.HOURS.toMillis(12)); // 12小時保留
        config.setCleanupIntervalMs(TimeUnit.MINUTES.toMillis(20)); // 20分鐘清理一次
        config.setMaxCountersSize(5000); // 較大的計數器限制
        config.setVerboseLoggingEnabled(false); // 關閉詳細日誌
        config.setHighPerformanceMode(true); // 啟用高性能模式
        config.setDefaultTimeoutMs(TimeUnit.SECONDS.toMillis(10)); // 較長超時時間
        return config;
    }

    /**
     * 測試環境配置 - 快速清理，完整監控
     */
    public static ProfilerConfig forTesting() {
        ProfilerConfig config = new ProfilerConfig();
        config.setDataRetentionTimeMs(TimeUnit.MINUTES.toMillis(10)); // 10分鐘保留
        config.setCleanupIntervalMs(TimeUnit.MINUTES.toMillis(2)); // 2分鐘清理一次
        config.setMaxCountersSize(50); // 小型限制便於測試
        config.setVerboseLoggingEnabled(true); // 完整日誌
        config.setDefaultTimeoutMs(TimeUnit.SECONDS.toMillis(1)); // 短超時便於測試
        return config;
    }

    /**
     * 高負載環境配置 - 優化性能，減少開銷
     */
    public static ProfilerConfig forHighLoad() {
        ProfilerConfig config = new ProfilerConfig();
        config.setDataRetentionTimeMs(TimeUnit.HOURS.toMillis(6)); // 6小時保留
        config.setCleanupIntervalMs(TimeUnit.MINUTES.toMillis(15)); // 15分鐘清理一次
        config.setMaxCountersSize(10000); // 大型限制
        config.setHighPerformanceMode(true); // 高性能模式
        config.setTimeoutLoggingEnabled(false); // 關閉超時日誌減少開銷
        config.setVerboseLoggingEnabled(false); // 關閉詳細日誌
        config.setBatchSize(500); // 較大批次處理
        return config;
    }

    // ==================== Fluent API 建造者模式 ====================

    public ProfilerConfig withDataRetention(long time, TimeUnit unit) {
        this.dataRetentionTimeMs = unit.toMillis(time);
        return this;
    }

    public ProfilerConfig withCleanupInterval(long interval, TimeUnit unit) {
        this.cleanupIntervalMs = unit.toMillis(interval);
        return this;
    }

    public ProfilerConfig withMaxCounters(int maxCounters) {
        this.maxCountersSize = maxCounters;
        return this;
    }

    public ProfilerConfig withDefaultTimeout(long timeout, TimeUnit unit) {
        this.defaultTimeoutMs = unit.toMillis(timeout);
        return this;
    }

    public ProfilerConfig withWorkerId(long workerId) {
        this.workerId = workerId;
        return this;
    }

    public ProfilerConfig enableVerboseLogging() {
        this.verboseLoggingEnabled = true;
        return this;
    }

    public ProfilerConfig disableVerboseLogging() {
        this.verboseLoggingEnabled = false;
        return this;
    }

    public ProfilerConfig enableHighPerformanceMode() {
        this.highPerformanceMode = true;
        return this;
    }

    public ProfilerConfig disableHighPerformanceMode() {
        this.highPerformanceMode = false;
        return this;
    }

    // ==================== 驗證方法 ====================

    /**
     * 驗證配置參數的有效性
     * 
     * @throws IllegalArgumentException 當配置無效時
     */
    public void validate() {
        if (dataRetentionTimeMs <= 0) {
            throw new IllegalArgumentException("Data retention time must be positive");
        }
        if (maxCountersSize <= 0) {
            throw new IllegalArgumentException("Max counters size must be positive");
        }
        if (cleanupIntervalMs <= 0) {
            throw new IllegalArgumentException("Cleanup interval must be positive");
        }
        if (defaultTimeoutMs <= 0) {
            throw new IllegalArgumentException("Default timeout must be positive");
        }
        if (workerId < 0 || workerId > 1023) {
            throw new IllegalArgumentException("Worker ID must be between 0 and 1023");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        // 邏輯驗證
        if (cleanupIntervalMs > dataRetentionTimeMs) {
            throw new IllegalArgumentException("Cleanup interval should not exceed data retention time");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 獲取配置摘要資訊
     */
    public String getSummary() {
        return String.format(
                "ProfilerConfig{retention=%dh, cleanup=%dm, maxCounters=%d, timeout=%ds, workerId=%d, " +
                        "verboseLog=%s, highPerf=%s, monitoring=%s}",
                TimeUnit.MILLISECONDS.toHours(dataRetentionTimeMs),
                TimeUnit.MILLISECONDS.toMinutes(cleanupIntervalMs),
                maxCountersSize,
                TimeUnit.MILLISECONDS.toSeconds(defaultTimeoutMs),
                workerId,
                verboseLoggingEnabled,
                highPerformanceMode,
                monitoringEnabled);
    }

    @Override
    public String toString() {
        return getSummary();
    }

    // ==================== Getter & Setter 方法 ====================

    public long getDataRetentionTimeMs() {
        return dataRetentionTimeMs;
    }

    public void setDataRetentionTimeMs(long dataRetentionTimeMs) {
        this.dataRetentionTimeMs = dataRetentionTimeMs;
    }

    public int getMaxCountersSize() {
        return maxCountersSize;
    }

    public void setMaxCountersSize(int maxCountersSize) {
        this.maxCountersSize = maxCountersSize;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public long getOrphanedCacheTimeMs() {
        return orphanedCacheTimeMs;
    }

    public void setOrphanedCacheTimeMs(long orphanedCacheTimeMs) {
        this.orphanedCacheTimeMs = orphanedCacheTimeMs;
    }

    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public boolean isTimeoutLoggingEnabled() {
        return timeoutLoggingEnabled;
    }

    public void setTimeoutLoggingEnabled(boolean timeoutLoggingEnabled) {
        this.timeoutLoggingEnabled = timeoutLoggingEnabled;
    }

    public boolean isTimeoutCountingEnabled() {
        return timeoutCountingEnabled;
    }

    public void setTimeoutCountingEnabled(boolean timeoutCountingEnabled) {
        this.timeoutCountingEnabled = timeoutCountingEnabled;
    }

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public boolean isVerboseLoggingEnabled() {
        return verboseLoggingEnabled;
    }

    public void setVerboseLoggingEnabled(boolean verboseLoggingEnabled) {
        this.verboseLoggingEnabled = verboseLoggingEnabled;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
    }

    public boolean isHighPerformanceMode() {
        return highPerformanceMode;
    }

    public void setHighPerformanceMode(boolean highPerformanceMode) {
        this.highPerformanceMode = highPerformanceMode;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getStatisticsFormat() {
        return statisticsFormat;
    }

    public void setStatisticsFormat(String statisticsFormat) {
        this.statisticsFormat = statisticsFormat;
    }
}
