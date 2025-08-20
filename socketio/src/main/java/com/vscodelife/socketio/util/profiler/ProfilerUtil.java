package com.vscodelife.socketio.util.profiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.util.SnowflakeUtil;
import com.vscodelife.socketio.util.SnowflakeUtil.SnowflakeGenerator;

/**
 * 高性能性能監測工具類
 * 
 * <p>
 * ProfilerUtil 是一個線程安全的性能監測工具，提供以下核心功能：
 * </p>
 * 
 * <h3>主要特性：</h3>
 * <ul>
 * <li><strong>線程安全</strong>：使用原子操作和併發安全的數據結構</li>
 * <li><strong>自動清理</strong>：支援自動清理過期和過量的監測數據</li>
 * <li><strong>靈活配置</strong>：支援多種環境配置（開發、生產、測試等）</li>
 * <li><strong>超時監控</strong>：自動記錄和統計超時操作</li>
 * <li><strong>統計分析</strong>：提供詳細的執行時間統計（最大、最小、平均）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 1. 基本使用 - 手動開始/結束
 * String executeName = ProfilerUtil.executeStart("database-query");
 * try {
 *     // 執行業務邏輯
 *     performDatabaseQuery();
 * } finally {
 *     ProfilerUtil.executeEnd("database-query", executeName, true);
 * }
 * 
 * // 2. 便捷使用 - 自動監測
 * ProfilerUtil.execute("api-call", () -> {
 *     // 執行業務邏輯
 *     return callExternalAPI();
 * }, true);
 * 
 * // 3. 查看統計結果
 * ProfilerUtil.showProfilers();
 * }</pre>
 * 
 * <h3>配置管理：</h3>
 * 
 * <pre>{@code
 * // 應用開發環境配置
 * ProfilerUtil.applyDevelopmentConfig();
 * 
 * // 啟用自動清理
 * ProfilerUtil.enableAutoCleanup();
 * 
 * // 自定義配置
 * ProfilerConfig config = new ProfilerConfig()
 *         .withDataRetention(2, TimeUnit.HOURS)
 *         .withMaxCounters(500)
 *         .enableVerboseLogging();
 * ProfilerUtil.setConfig(config);
 * }</pre>
 * 
 * <h3>線程安全性：</h3>
 * <p>
 * 本工具類所有公開方法都是線程安全的，可以在多線程環境中安全使用。
 * 內部使用 {@link ProfilerCounterManager} 來管理原子操作和數據一致性。
 * </p>
 * 
 * <h3>性能考量：</h3>
 * <ul>
 * <li>使用 {@link ConcurrentHashMap} 和原子操作，最小化鎖競爭</li>
 * <li>支援高性能模式，可關閉詳細日誌以減少開銷</li>
 * <li>自動清理機制避免記憶體洩漏</li>
 * </ul>
 * 
 * @author vscodelife
 * @version 2.0
 * @since 1.0
 * @see ProfilerCounterManager 底層原子操作管理器
 * @see ProfilerConfig 配置管理類
 * @see ProfilerCounter 性能計數器
 * @thread-safe 本類是線程安全的
 */
public final class ProfilerUtil {
    /** 類級別日誌記錄器 */
    private static final Logger logger = LoggerFactory.getLogger(ProfilerUtil.class);

    /**
     * 私有建構函數，防止工具類被實例化
     * 
     * @throws UnsupportedOperationException 總是拋出此異常
     */
    private ProfilerUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** 執行快取，用於暫存監測操作的開始時間戳，鍵為執行名稱，值為開始時間 */
    private static Map<String, Long> profilerCache = new ConcurrentHashMap<>();

    /** 性能計數器管理器，負責線程安全的原子操作和數據一致性管理 */
    private static ProfilerCounterManager profilerManager = new ProfilerCounterManager();

    /** 雪花算法ID生成器，用於生成唯一的監測執行名稱 */
    private static SnowflakeGenerator idGenerator = SnowflakeUtil.createGenerator(1);

    // ==================== 配置管理相關字段 ====================

    /** 當前使用的配置實例，包含所有性能監測相關的參數設定 */
    private static ProfilerConfig currentConfig = new ProfilerConfig(); // 預設配置

    // ==================== 自動清理機制相關字段 ====================

    /** 自動清理任務的執行緒池調度器 */
    private static ScheduledExecutorService cleanupScheduler = null;

    /** 自動清理機制啟用狀態的原子布林值，確保線程安全的狀態切換 */
    private static final AtomicBoolean cleanupEnabled = new AtomicBoolean(false);

    /**
     * 取得所有監測計數器的列表
     * 
     * <p>
     * 此方法返回當前系統中所有活躍的性能計數器。返回的列表是當前狀態的快照，
     * 不會因為後續的操作而改變。
     * </p>
     * 
     * @return 包含所有 {@link ProfilerCounter} 的不可變列表
     * @see ProfilerCounter
     * @see ProfilerCounterManager#getAllCounters()
     */
    public static List<ProfilerCounter> getCounters() {
        return profilerManager.getAllCounters();
    }

    // ==================== 配置管理方法 ====================

    /**
     * 設定 ProfilerUtil 的全局配置
     * 
     * <p>
     * 此方法用於更新系統的配置參數，包括數據保留時間、清理間隔、超時設定等。
     * 配置更新會立即生效，並且如果自動清理機制已啟用且相關參數改變，
     * 會自動重啟清理機制以應用新的配置。
     * </p>
     * 
     * <h4>配置更新影響：</h4>
     * <ul>
     * <li>如果 workerId 改變，會重新初始化雪花算法ID生成器</li>
     * <li>如果清理相關參數改變，會重啟自動清理機制</li>
     * <li>所有後續的監測操作都會使用新配置</li>
     * </ul>
     * 
     * @param config 新的配置實例，不能為 null
     * @throws IllegalArgumentException 當 config 為 null 或配置驗證失敗時
     * @see ProfilerConfig#validate() 配置驗證邏輯
     * @see #enableAutoCleanup() 自動清理機制
     */
    public static void setConfig(ProfilerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        // 驗證配置
        config.validate();

        // 更新配置
        ProfilerConfig oldConfig = currentConfig;
        currentConfig = new ProfilerConfig(config); // 複製配置

        // 重新初始化 ID 生成器如果 workerId 改變
        if (oldConfig.getWorkerId() != config.getWorkerId()) {
            idGenerator = SnowflakeUtil.createGenerator(config.getWorkerId());
        }

        // 如果清理機制已啟用且參數改變，需要重啟清理機制
        if (cleanupEnabled.get()) {
            boolean needRestart = (oldConfig.getCleanupIntervalMs() != config.getCleanupIntervalMs() ||
                    oldConfig.getDataRetentionTimeMs() != config.getDataRetentionTimeMs() ||
                    oldConfig.getMaxCountersSize() != config.getMaxCountersSize());

            if (needRestart) {
                logger.info("Config changed, restarting cleanup mechanism");
                disableAutoCleanup();
                enableAutoCleanup();
            }
        }

        logger.info("ProfilerUtil configuration updated: {}", currentConfig.getSummary());
    }

    /**
     * 獲取當前配置的副本
     * 
     * <p>
     * 返回當前使用配置的深度複製，修改返回的配置對象不會影響系統當前的配置。
     * 這提供了一種安全的方式來檢查當前配置狀態。
     * </p>
     * 
     * @return 當前配置的副本，永遠不會返回 null
     * @see ProfilerConfig 配置類說明
     */
    public static ProfilerConfig getConfig() {
        return new ProfilerConfig(currentConfig);
    }

    /**
     * 重置為預設配置
     * 
     * <p>
     * 將系統配置重置為初始預設值。這會覆蓋所有當前的自定義配置，
     * 並且如果自動清理機制正在運行，會根據新配置重啟。
     * </p>
     * 
     * @see ProfilerConfig#ProfilerConfig() 預設配置值
     */
    public static void resetToDefaultConfig() {
        setConfig(new ProfilerConfig());
    }

    /**
     * 應用開發環境配置
     * 
     * <p>
     * 配置特點：
     * </p>
     * <ul>
     * <li>較短的數據保留時間（30分鐘）</li>
     * <li>較頻繁的清理間隔（5分鐘）</li>
     * <li>較小的計數器限制（100個）</li>
     * <li>啟用詳細日誌</li>
     * <li>較短的超時時間（2秒）</li>
     * </ul>
     * 
     * @see ProfilerConfig#forDevelopment() 開發環境配置詳情
     */
    public static void applyDevelopmentConfig() {
        setConfig(ProfilerConfig.forDevelopment());
    }

    /**
     * 應用生產環境配置
     * 
     * <p>
     * 配置特點：
     * </p>
     * <ul>
     * <li>較長的數據保留時間（12小時）</li>
     * <li>適中的清理間隔（20分鐘）</li>
     * <li>較大的計數器限制（5000個）</li>
     * <li>關閉詳細日誌以提升性能</li>
     * <li>啟用高性能模式</li>
     * <li>較長的超時時間（10秒）</li>
     * </ul>
     * 
     * @see ProfilerConfig#forProduction() 生產環境配置詳情
     */
    public static void applyProductionConfig() {
        setConfig(ProfilerConfig.forProduction());
    }

    /**
     * 應用測試環境配置
     * 
     * <p>
     * 配置特點：
     * </p>
     * <ul>
     * <li>很短的數據保留時間（10分鐘）</li>
     * <li>很頻繁的清理間隔（2分鐘）</li>
     * <li>小型計數器限制（50個）</li>
     * <li>啟用完整日誌</li>
     * <li>很短的超時時間（1秒）</li>
     * </ul>
     * 
     * @see ProfilerConfig#forTesting() 測試環境配置詳情
     */
    public static void applyTestingConfig() {
        setConfig(ProfilerConfig.forTesting());
    }

    /**
     * 應用高負載環境配置
     * 
     * <p>
     * 配置特點：
     * </p>
     * <ul>
     * <li>中等的數據保留時間（6小時）</li>
     * <li>適中的清理間隔（15分鐘）</li>
     * <li>大型計數器限制（10000個）</li>
     * <li>啟用高性能模式</li>
     * <li>關閉超時和詳細日誌以減少開銷</li>
     * <li>較大的批次處理大小（500）</li>
     * </ul>
     * 
     * @see ProfilerConfig#forHighLoad() 高負載環境配置詳情
     */
    public static void applyHighLoadConfig() {
        setConfig(ProfilerConfig.forHighLoad());
    }

    /**
     * 啟用自動清理機制（自定義參數）
     * 
     * <p>
     * 啟用自動清理機制，定期清理過期和過量的監測數據，防止記憶體洩漏。
     * 清理機制在後台執行，不會影響正常的監測操作。
     * </p>
     * 
     * <h4>清理策略：</h4>
     * <ol>
     * <li>清理創建時間超過保留期的計數器</li>
     * <li>如果計數器數量仍超過限制，清理最舊的計數器</li>
     * <li>清理孤立的快取項目（超過1小時的暫存）</li>
     * </ol>
     * 
     * <p>
     * <strong>注意：</strong>此方法會更新當前配置中的相關參數，
     * 並且如果清理機制已經啟用，會重啟以應用新參數。
     * </p>
     * 
     * @param retentionTimeMs   數據保留時間(毫秒)，必須 > 0
     * @param maxCounters       最大計數器數量，必須 > 0
     * @param cleanupIntervalMs 清理間隔時間(毫秒)，必須 > 0
     * @throws IllegalArgumentException 當參數無效時
     * @see #disableAutoCleanup() 停用清理機制
     * @see #manualCleanup() 手動執行清理
     */
    public static void enableAutoCleanup(long retentionTimeMs, int maxCounters, long cleanupIntervalMs) {
        // 更新配置
        ProfilerConfig newConfig = new ProfilerConfig(currentConfig);
        newConfig.setDataRetentionTimeMs(retentionTimeMs);
        newConfig.setMaxCountersSize(maxCounters);
        newConfig.setCleanupIntervalMs(cleanupIntervalMs);
        setConfig(newConfig);

        if (cleanupEnabled.compareAndSet(false, true)) {
            // 創建新的執行緒池
            if (cleanupScheduler == null || cleanupScheduler.isShutdown()) {
                cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "ProfilerUtil-Cleanup");
                    t.setDaemon(true);
                    return t;
                });
            }

            cleanupScheduler.scheduleAtFixedRate(
                    ProfilerUtil::performCleanup,
                    currentConfig.getCleanupIntervalMs(),
                    currentConfig.getCleanupIntervalMs(),
                    TimeUnit.MILLISECONDS);
            logger.info("ProfilerUtil auto cleanup enabled - retention: {}ms, maxCounters: {}, interval: {}ms",
                    retentionTimeMs, maxCounters, cleanupIntervalMs);
        }
    }

    /**
     * 啟用自動清理機制（使用當前配置參數）
     * 
     * <p>
     * 使用當前配置中的清理參數啟用自動清理機制：
     * </p>
     * <ul>
     * <li>預設保留24小時</li>
     * <li>預設最大1000個計數器</li>
     * <li>預設30分鐘清理一次</li>
     * </ul>
     * 
     * <p>
     * 這是最常用的啟用方式，適合大多數應用場景。
     * </p>
     * 
     * @see #enableAutoCleanup(long, int, long) 自定義參數版本
     */
    public static void enableAutoCleanup() {
        enableAutoCleanup(
                currentConfig.getDataRetentionTimeMs(),
                currentConfig.getMaxCountersSize(),
                currentConfig.getCleanupIntervalMs());
    }

    /**
     * 停用自動清理機制
     * 
     * <p>
     * 停止自動清理的後台任務，釋放相關資源。已經存在的監測數據不會被清理，
     * 需要手動調用 {@link #manualCleanup()} 或 {@link #clearAllData()} 進行清理。
     * </p>
     * 
     * <p>
     * 停用過程是線程安全的，會等待當前正在執行的清理任務完成。
     * </p>
     * 
     * @see #enableAutoCleanup() 重新啟用清理機制
     */
    public static void disableAutoCleanup() {
        if (cleanupEnabled.compareAndSet(true, false)) {
            if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
                cleanupScheduler.shutdown();
                try {
                    if (!cleanupScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                        cleanupScheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupScheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            logger.info("ProfilerUtil auto cleanup disabled");
        }
    }

    /**
     * 手動執行清理操作
     * 
     * <p>
     * 立即執行一次清理操作，清理過期和過量的監測數據。
     * 此方法可以在自動清理機制啟用或停用時調用。
     * </p>
     * 
     * @return 被清理的項目數量（包括計數器和快取項目）
     * @see #enableAutoCleanup() 自動清理機制
     */
    public static int manualCleanup() {
        return performCleanup();
    }

    /**
     * 清空所有監測數據
     * 
     * <p>
     * 清除所有監測計數器和快取數據，重置系統到初始狀態。
     * 此操作不可逆，主要用於測試環境或系統重置。
     * </p>
     * 
     * <p>
     * <strong>警告：</strong>此操作會丟失所有統計數據！
     * </p>
     * 
     * @see #manualCleanup() 漸進式清理
     */
    public static void clearAllData() {
        profilerManager.clearAll();
        profilerCache.clear();
    }

    /**
     * 執行清理操作
     * 
     * @return 清理的項目數量
     */
    private static int performCleanup() {
        int cleanedCount = 0;
        long currentTime = System.currentTimeMillis();

        try {
            // 1. 清理過期的監測數據
            cleanedCount += cleanupExpiredData(currentTime);

            // 2. 如果數量仍然超過限制，清理最舊的數據
            if (profilerManager.getCounterSize() > currentConfig.getMaxCountersSize()) {
                cleanedCount += cleanupOldestData();
            }

            // 3. 清理孤立的快取數據(超過1小時的暫存)
            cleanedCount += cleanupOrphanedCache(currentTime);

            if (cleanedCount > 0) {
                logger.debug(
                        "ProfilerUtil cleanup completed - removed {} items, remaining counters: {}, remaining cache: {}",
                        cleanedCount, profilerManager.getCounterSize(), profilerCache.size());
            }

        } catch (Exception e) {
            logger.error("ProfilerUtil cleanup failed", e);
        }

        return cleanedCount;
    }

    /**
     * 清理過期的監測數據
     */
    private static int cleanupExpiredData(long currentTime) {
        long expireTime = currentTime - currentConfig.getDataRetentionTimeMs();
        return profilerManager.removeExpiredCounters(expireTime);
    }

    /**
     * 清理最舊的數據(當數量超過限制時)
     */
    private static int cleanupOldestData() {
        int excessCount = profilerManager.getCounterSize() - currentConfig.getMaxCountersSize();

        if (excessCount > 0) {
            return profilerManager.removeOldestCounters(excessCount);
        }

        return 0;
    }

    /**
     * 清理孤立的快取數據(超過1小時的暫存)
     */
    private static int cleanupOrphanedCache(long currentTime) {
        int cleanedCount = 0;
        long cacheExpireTime = currentTime - currentConfig.getOrphanedCacheTimeMs();

        Set<String> expiredCacheKeys = profilerCache.entrySet().stream()
                .filter(entry -> entry.getValue() < cacheExpireTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (String key : expiredCacheKeys) {
            profilerCache.remove(key);
            cleanedCount++;
        }

        return cleanedCount;
    }

    /**
     * 獲取清理機制狀態資訊
     * 
     * <p>
     * 返回當前清理機制的詳細狀態資訊，包括：
     * </p>
     * <ul>
     * <li>清理機制啟用狀態</li>
     * <li>當前計數器數量和限制</li>
     * <li>快取項目數量</li>
     * <li>數據保留時間</li>
     * <li>清理間隔時間</li>
     * </ul>
     * 
     * @return 格式化的狀態資訊字串
     * @see #enableAutoCleanup() 啟用清理機制
     */
    public static String getCleanupStatus() {
        return String.format(
                "Cleanup Status: enabled=%s, counters=%d/%d, cache=%d, retention=%dh, interval=%dm",
                cleanupEnabled.get(),
                profilerManager.getCounterSize(), currentConfig.getMaxCountersSize(),
                profilerCache.size(),
                TimeUnit.MILLISECONDS.toHours(currentConfig.getDataRetentionTimeMs()),
                TimeUnit.MILLISECONDS.toMinutes(currentConfig.getCleanupIntervalMs()));
    }

    /**
     * 產生唯一的執行監測名稱
     * 
     * <p>
     * 為監測操作生成唯一的執行名稱，使用雪花算法確保唯一性。
     * 如果雪花算法失敗，會降級使用當前時間戳。
     * </p>
     * 
     * <p>
     * 生成的名稱格式：{profilerName}-{唯一ID}
     * </p>
     * 
     * @param profilername 監測物件的基礎名稱，不能為 null 或空字串
     * @return 唯一的執行監測名稱，格式為 "profilername-id"
     * @see SnowflakeUtil 雪花算法實現
     */
    public static String genProfilerName(String profilername) {
        String profilerName = null;
        try {
            profilerName = String.format("%s-%d", profilername, idGenerator.nextId());
        } catch (Exception e) {
            profilerName = String.format("%s-%d", profilername, System.currentTimeMillis());
            logger.info("gen profilername fail - {}", e.getMessage());
        }
        return profilerName;
    }

    /**
     * 開始監測操作
     * 
     * <p>
     * 記錄監測操作的開始時間，返回唯一的執行名稱用於後續的結束操作。
     * 開始時間會被暫存在內部快取中，直到調用對應的 executeEnd 方法。
     * </p>
     * 
     * <h4>使用模式：</h4>
     * 
     * <pre>{@code
     * String executeName = ProfilerUtil.executeStart("database-operation");
     * try {
     *     // 執行業務邏輯
     *     performDatabaseOperation();
     * } finally {
     *     ProfilerUtil.executeEnd("database-operation", executeName, true);
     * }
     * }</pre>
     * 
     * @param profilerName 監測操作的名稱，用於分類統計
     * @return 唯一的執行名稱，用於對應的 executeEnd 調用
     * @see #executeEnd(String, String, boolean) 結束監測
     * @see #genProfilerName(String) 名稱生成邏輯
     */
    public static String executeStart(String profilerName) {
        String executeName = genProfilerName(profilerName);
        if (!profilerCache.containsKey(executeName)) {
            profilerCache.put(executeName, System.currentTimeMillis());
        }
        return executeName;
    }

    /**
     * 結束監測操作（使用配置中的預設超時時間）
     * 
     * <p>
     * 結束之前通過 {@link #executeStart(String)} 開始的監測操作。
     * 會計算執行時間並根據 counted 參數決定是否統計到性能計數器中。
     * </p>
     * 
     * <p>
     * 使用當前配置中的預設超時時間來判斷是否記錄超時日誌。
     * </p>
     * 
     * @param profilerName 監測物件名稱，必須與 executeStart 中使用的名稱一致
     * @param executeName  執行監測名稱，由 executeStart 方法返回
     * @param counted      是否將此次執行統計到計數器中（true=統計，false=僅檢查超時）
     * @see #executeStart(String) 開始監測
     * @see ProfilerConfig#getDefaultTimeoutMs() 預設超時時間
     */
    public static void executeEnd(String profilerName, String executeName, boolean counted) {
        executeEnd(profilerName, executeName, null, null, currentConfig.getDefaultTimeoutMs(), counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, long timeout,
            boolean counted) {
        executeEnd(profilerName, executeName, null, null, timeout, counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param logFunc      log事件
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, Supplier<String> logFunc,
            boolean counted) {
        executeEnd(profilerName, executeName, logFunc, null, 0, counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param exeFunc      執行完畢事件
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, Consumer<String> exeFunc,
            boolean counted) {
        executeEnd(profilerName, executeName, null, exeFunc, 0, counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param logFunc      執行完畢事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, Supplier<String> logFunc,
            long timeout, boolean counted) {
        executeEnd(profilerName, executeName, logFunc, null, timeout, counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param exeFunc      執行完畢事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, Consumer<String> exeFunc,
            long timeout, boolean counted) {
        executeEnd(profilerName, executeName, null, exeFunc, timeout, counted);
    }

    /**
     * 監測結束
     * 
     * @param profilerName 監測物件名稱
     * @param executeName  執行監測名稱
     * @param logFunc      log事件
     * @param exeFunc      執行完畢事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void executeEnd(String profilerName, String executeName, Supplier<String> logFunc,
            Consumer<String> exeFunc, long timeout, boolean counted) {
        if (counted) {
            // 檢查是否有統計物件，如果沒有則創建
            ProfilerCounter counter = profilerManager.getOrCreateCounter(profilerName);

            // 檢查暫存
            if (profilerCache.containsKey(executeName)) {
                long timestamp1 = profilerCache.get(executeName);
                long timestamp2 = System.currentTimeMillis();
                long executeTime = timestamp2 - timestamp1;
                // 執行統計
                counter.execute(executeTime, executeTime >= timeout && timeout > 0);
                // 檢查時間
                if (executeTime >= timeout && timeout > 0) {
                    log(executeName, executeTime, logFunc, exeFunc);
                }
                // 移除暫存
                profilerCache.remove(executeName);
            }
        } else {
            // 檢查暫存
            if (profilerCache.containsKey(executeName)) {
                long timestamp1 = profilerCache.get(executeName);
                long timestamp2 = System.currentTimeMillis();
                long executeTime = timestamp2 - timestamp1;
                // 檢查時間
                if (executeTime >= timeout && timeout > 0) {
                    log(executeName, executeTime, logFunc, exeFunc);
                }
                // 移除暫存
                profilerCache.remove(executeName);
            }
        }
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, boolean counted) {
        execute(profilerName, func, null, null, 0, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, long timeout,
            boolean counted) {
        execute(profilerName, func, null, null, timeout, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param logFunc      log事件
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, Supplier<String> logFunc,
            boolean counted) {
        execute(profilerName, func, logFunc, null, 0, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param exeFunc      執行完畢事件
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, Consumer<String> exeFunc,
            boolean counted) {
        execute(profilerName, func, null, exeFunc, 0, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param logFunc      log事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, Supplier<String> logFunc,
            long timeout, boolean counted) {
        execute(profilerName, func, logFunc, null, timeout, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param exeFunc      執行完畢事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, Consumer<String> exeFunc,
            long timeout, boolean counted) {
        execute(profilerName, func, null, exeFunc, timeout, counted);
    }

    /**
     * 執行監測
     * 
     * @param profilerName 監測物件名稱
     * @param func         監測事件
     * @param logFunc      log事件
     * @param exeFunc      執行完畢事件
     * @param timeout      逾時時間
     * @param counted      統計標記
     */
    public static void execute(String profilerName, Supplier<String> func, Supplier<String> logFunc,
            Consumer<String> exeFunc, long timeout, boolean counted) {
        if (counted) {
            // 檢查是否有統計物件，如果沒有則創建
            ProfilerCounter counter = profilerManager.getOrCreateCounter(profilerName);

            // 檢查執行事件
            if (func != null) {
                String executeName = null;
                long timestamp1 = System.currentTimeMillis();
                try {
                    executeName = func.get();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                long timestamp2 = System.currentTimeMillis();
                long executeTime = timestamp2 - timestamp1;
                // 執行統計
                counter.execute(executeTime, executeTime >= timeout && timeout > 0);
                // 檢查時間
                if (executeTime >= timeout && timeout > 0) {
                    log(executeName, executeTime, logFunc, exeFunc);
                }
            }
        } else {
            // 檢查執行事件
            if (func != null) {
                String executeName = null;
                long timestamp1 = System.currentTimeMillis();
                try {
                    executeName = func.get();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                long timestamp2 = System.currentTimeMillis();
                long executeTime = timestamp2 - timestamp1;
                // 檢查時間
                if (executeTime >= timeout && timeout > 0) {
                    log(executeName, executeTime, logFunc, exeFunc);
                }
            }
        }
    }

    /**
     * 預設log事件
     * 
     * @param executeName 執行監測名稱
     * @param executeTime 執行時間
     * @param logFunc     log事件
     * @param exeFunc     執行完畢事件
     */
    private static void log(String executeName, long executeTime, Supplier<String> logFunc,
            Consumer<String> exeFunc) {
        try {
            // 檢查是否啟用超時日誌記錄
            if (!currentConfig.isTimeoutLoggingEnabled()) {
                return;
            }

            if (exeFunc != null) {
                String str = null;
                if (logFunc != null) {
                    str = String.format("profiler execute [%s] time %d other params = %s",
                            executeName, executeTime, logFunc.get());
                } else {
                    str = String.format("profiler execute [%s] time %d", executeName, executeTime);
                }
                exeFunc.accept(str);
            } else {
                if (currentConfig.isVerboseLoggingEnabled()) {
                    // 詳細日誌模式
                    if (logFunc != null) {
                        logger.info("profiler execute [{}] time {} ms (timeout exceeded) - params: {}",
                                executeName, executeTime, logFunc.get());
                    } else {
                        logger.info("profiler execute [{}] time {} ms (timeout exceeded)",
                                executeName, executeTime);
                    }
                } else {
                    // 簡潔日誌模式
                    if (logFunc != null) {
                        logger.info("profiler execute [{}] time {} other params = {}", executeName,
                                executeTime, logFunc.get());
                    } else {
                        logger.info("profiler execute [{}] time {}", executeName, executeTime);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 顯示所有監測數據（按名稱排序）
     * 
     * <p>
     * 將所有性能計數器的統計資訊輸出到控制台，按監測名稱字母順序排序。
     * 輸出格式包括執行次數、超時次數、最大/最小/平均執行時間等統計資訊。
     * </p>
     * 
     * @see #showProfilersSortedBy(String) 自定義排序方式
     */
    public static void showProfilers() {
        showProfilersSortedBy("name");
    }

    /**
     * 顯示監測數據（按指定方式排序）
     * 
     * <p>
     * 將所有性能計數器的統計資訊按指定方式排序後輸出到控制台。
     * 支援多種排序方式，便於分析不同維度的性能數據。
     * </p>
     * 
     * <h4>支援的排序方式：</h4>
     * <ul>
     * <li><strong>name</strong> - 按名稱字母順序排序（升序）</li>
     * <li><strong>count</strong> - 按執行次數排序（降序，最多執行的在前）</li>
     * <li><strong>max</strong> - 按最大執行時間排序（降序，最耗時的在前）</li>
     * <li><strong>min</strong> - 按最小執行時間排序（升序，最快的在前）</li>
     * <li><strong>average</strong> - 按平均執行時間排序（降序，平均最慢的在前）</li>
     * </ul>
     * 
     * @param sortBy 排序方式，不區分大小寫，無效值時預設按名稱排序
     * @see ProfilerCounter#toString() 輸出格式說明
     */
    public static void showProfilersSortedBy(String sortBy) {
        profilerManager.getAllCounters().stream()
                .sorted((c1, c2) -> {
                    switch (sortBy.toLowerCase()) {
                        case "count":
                            return Long.compare(c2.getExecuteCount(), c1.getExecuteCount()); // 降序
                        case "max":
                            return Long.compare(c2.getMaxExecuteTime(), c1.getMaxExecuteTime()); // 降序
                        case "min":
                            return Long.compare(c1.getMinExecuteTime(), c2.getMinExecuteTime()); // 升序
                        case "average":
                            return Long.compare(c2.getAverageExecuteTime(), c1.getAverageExecuteTime()); // 降序
                        case "name":
                        default:
                            return c1.getName().compareTo(c2.getName()); // 按名稱升序
                    }
                })
                .forEach(counter -> {
                    System.out.println(counter.toString());
                });
    }

}
