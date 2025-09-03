package com.vscodelife.socketio.util.profiler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 性能計數器和時間戳管理器
 * 
 * <p>
 * 提供線程安全的原子性操作，確保 ProfilerCounter 和創建時間戳的一致性管理。
 * 本類使用雙重保護機制來實現高效的併發安全：
 * </p>
 * 
 * <h3>線程安全策略：</h3>
 * <ul>
 * <li><strong>第一層保護</strong>：{@link ConcurrentHashMap} 提供基礎的線程安全讀寫</li>
 * <li><strong>第二層保護</strong>：{@link ReadWriteLock} 確保複合操作的原子性</li>
 * </ul>
 * 
 * <h3>核心設計理念：</h3>
 * <ul>
 * <li><strong>數據一致性</strong>：確保每個 ProfilerCounter 都有對應的時間戳</li>
 * <li><strong>原子性操作</strong>：創建、刪除操作保證原子性，避免數據不一致</li>
 * <li><strong>高性能讀取</strong>：使用雙重檢查鎖定模式，優化常見的讀取場景</li>
 * <li><strong>批量操作</strong>：支援批量清理，提升大數據量場景下的性能</li>
 * </ul>
 * 
 * <h3>使用場景：</h3>
 * 
 * <pre>{@code
 * ProfilerCounterManager manager = new ProfilerCounterManager();
 * 
 * // 線程安全的獲取或創建
 * ProfilerCounter counter = manager.getOrCreateCounter("api-call");
 * counter.execute(150, false);
 * 
 * // 批量清理過期數據
 * int cleaned = manager.removeExpiredCounters(System.currentTimeMillis() - 3600000);
 * 
 * // 檢查數據一致性
 * if (!manager.checkConsistency()) {
 *     manager.repairConsistency();
 * }
 * }</pre>
 * 
 * <h3>性能特點：</h3>
 * <ul>
 * <li>讀取操作無鎖競爭（在數據已存在的情況下）</li>
 * <li>寫入操作使用最小粒度的鎖定</li>
 * <li>支援併發讀取，序列化複合寫入</li>
 * <li>內存效率高，無額外的同步開銷</li>
 * </ul>
 * 
 * @author vscodelife
 * @version 2.0
 * @since 1.0
 * @see ProfilerCounter 性能計數器
 * @see ProfilerUtil 主要使用者類
 * @thread-safe 本類是線程安全的
 */
public class ProfilerCounterManager {

    /** 性能計數器存儲，鍵為計數器名稱，值為計數器實例 */
    private final Map<String, ProfilerCounter> profilerCounters = new ConcurrentHashMap<>();

    /** 創建時間戳存儲，鍵為計數器名稱，值為創建時間戳 */
    private final Map<String, Long> profilerTimestamps = new ConcurrentHashMap<>();

    /** 讀寫鎖，用於保證複合操作的原子性，優化讀多寫少的場景 */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 原子性創建或獲取 ProfilerCounter
     * 
     * <p>
     * 這是本類最核心的方法，使用雙重檢查鎖定模式確保高效且線程安全的操作：
     * </p>
     * 
     * <h4>執行邏輯：</h4>
     * <ol>
     * <li><strong>第一次檢查</strong>：無鎖檢查 counter 是否已存在（快速路徑）</li>
     * <li><strong>獲取寫鎖</strong>：如果不存在，獲取寫鎖進行創建</li>
     * <li><strong>第二次檢查</strong>：再次檢查避免重複創建（雙重檢查）</li>
     * <li><strong>原子創建</strong>：同時創建 counter 和 timestamp，確保一致性</li>
     * </ol>
     * 
     * <p>
     * <strong>性能優化：</strong>在絕大多數情況下（counter 已存在），
     * 此方法只需要一次 HashMap 查找，無鎖競爭。
     * </p>
     * 
     * @param profilerName 監測物件名稱，不能為 null 或空字串
     * @return ProfilerCounter 實例，永遠不會返回 null
     * @throws IllegalArgumentException 當 profilerName 無效時
     * @see ProfilerCounter 返回的計數器類型
     */
    public ProfilerCounter getOrCreateCounter(String profilerName) {
        // 首先嘗試快速讀取（大多數情況下 counter 已存在）
        ProfilerCounter counter = profilerCounters.get(profilerName);
        if (counter != null) {
            return counter;
        }

        // 如果不存在，需要原子性創建
        lock.writeLock().lock();
        try {
            // 雙重檢查模式，防止重複創建
            counter = profilerCounters.get(profilerName);
            if (counter != null) {
                return counter;
            }

            // 原子性創建 counter 和 timestamp
            counter = new ProfilerCounter(profilerName);
            long currentTime = System.currentTimeMillis();

            profilerCounters.put(profilerName, counter);
            profilerTimestamps.put(profilerName, currentTime);

            return counter;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取現有的 ProfilerCounter（只讀操作）
     * 
     * <p>
     * 此方法只查找現有的計數器，不會創建新的。
     * 適用於檢查計數器是否存在，或者獲取已知存在的計數器。
     * </p>
     * 
     * @param profilerName 監測物件名稱
     * @return ProfilerCounter 實例，如果不存在則返回 null
     * @see #getOrCreateCounter(String) 如果需要確保存在請使用此方法
     */
    public ProfilerCounter getCounter(String profilerName) {
        return profilerCounters.get(profilerName);
    }

    /**
     * 檢查是否存在指定的 ProfilerCounter
     * 
     * @param profilerName 監測物件名稱
     * @return 如果存在返回 true，否則返回 false
     */
    public boolean containsCounter(String profilerName) {
        return profilerCounters.containsKey(profilerName);
    }

    /**
     * 獲取所有 ProfilerCounter 的快照列表
     * 
     * <p>
     * 返回當前所有計數器的副本列表，列表內容不會因後續操作而改變。
     * 此方法使用讀鎖保護，確保獲取的數據一致性。
     * </p>
     * 
     * <p>
     * <strong>注意：</strong>返回的是快照，不是實時視圖。
     * </p>
     * 
     * @return 包含所有 ProfilerCounter 的不可變列表
     */
    public List<ProfilerCounter> getAllCounters() {
        lock.readLock().lock();
        try {
            return profilerCounters.values().stream().collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取所有 ProfilerCounter 名稱的快照集合
     * 
     * <p>
     * 返回當前所有計數器名稱的副本集合。
     * </p>
     * 
     * @return 包含所有計數器名稱的不可變集合
     */
    public Set<String> getAllCounterNames() {
        lock.readLock().lock();
        try {
            return profilerCounters.keySet().stream().collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取指定 ProfilerCounter 的創建時間戳
     * 
     * @param profilerName 監測物件名稱
     * @return 創建時間戳（毫秒），如果不存在返回 null
     */
    public Long getTimestamp(String profilerName) {
        return profilerTimestamps.get(profilerName);
    }

    /**
     * 原子性移除指定的 ProfilerCounter 和對應時間戳
     * 
     * <p>
     * 確保計數器和時間戳同時被移除，維持數據一致性。
     * </p>
     * 
     * @param profilerName 監測物件名稱
     * @return 被移除的 ProfilerCounter 實例，如果不存在返回 null
     */
    public ProfilerCounter removeCounter(String profilerName) {
        lock.writeLock().lock();
        try {
            ProfilerCounter removedCounter = profilerCounters.remove(profilerName);
            profilerTimestamps.remove(profilerName);
            return removedCounter;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量移除過期的 ProfilerCounter
     * 
     * <p>
     * 此方法用於自動清理機制，移除創建時間早於指定時間的所有計數器。
     * 採用流式處理和批量操作，提升大數據量場景下的性能。
     * </p>
     * 
     * <h4>執行步驟：</h4>
     * <ol>
     * <li>篩選出過期的時間戳條目</li>
     * <li>收集對應的鍵值</li>
     * <li>批量移除計數器和時間戳</li>
     * </ol>
     * 
     * @param expireTime 過期時間戳，早於此時間的計數器將被移除
     * @return 實際移除的計數器數量
     * @see ProfilerUtil#performCleanup() 自動清理機制
     */
    public int removeExpiredCounters(long expireTime) {
        lock.writeLock().lock();
        try {
            Set<String> expiredKeys = profilerTimestamps.entrySet().stream()
                    .filter(entry -> entry.getValue() < expireTime)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            int removedCount = 0;
            for (String key : expiredKeys) {
                if (profilerCounters.remove(key) != null) {
                    profilerTimestamps.remove(key);
                    removedCount++;
                }
            }

            return removedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量移除最舊的 ProfilerCounter
     * 
     * <p>
     * 按創建時間排序，移除最舊的計數器直到達到指定數量。
     * 主要用於當計數器數量超過限制時的清理操作。
     * </p>
     * 
     * <h4>清理策略：</h4>
     * <ul>
     * <li>按時間戳升序排序（最舊的在前）</li>
     * <li>移除指定數量的最舊項目</li>
     * <li>保持數據一致性</li>
     * </ul>
     * 
     * @param maxToRemove 最多移除的數量，必須 >= 0
     * @return 實際移除的計數器數量
     */
    public int removeOldestCounters(int maxToRemove) {
        if (maxToRemove <= 0) {
            return 0;
        }

        lock.writeLock().lock();
        try {
            List<String> oldestKeys = profilerTimestamps.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(maxToRemove)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            int removedCount = 0;
            for (String key : oldestKeys) {
                if (profilerCounters.remove(key) != null) {
                    profilerTimestamps.remove(key);
                    removedCount++;
                }
            }

            return removedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空所有 ProfilerCounter 和時間戳
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            profilerCounters.clear();
            profilerTimestamps.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 獲取當前 ProfilerCounter 的數量
     * 
     * @return 計數器數量
     */
    public int getCounterSize() {
        return profilerCounters.size();
    }

    /**
     * 獲取管理器狀態資訊
     * 
     * @return 狀態資訊字串
     */
    public String getStatus() {
        lock.readLock().lock();
        try {
            return String.format("ProfilerCounterManager: counters=%d, timestamps=%d",
                    profilerCounters.size(), profilerTimestamps.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 獲取按時間戳排序的計數器列表（最新的在前）
     * 
     * @return 排序後的計數器列表
     */
    public List<ProfilerCounter> getCountersSortedByTime() {
        lock.readLock().lock();
        try {
            return profilerTimestamps.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(entry -> profilerCounters.get(entry.getKey()))
                    .filter(counter -> counter != null)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 檢查數據一致性
     * 驗證 profilerCounters 和 profilerTimestamps 是否同步
     * 
     * @return 如果一致返回 true，否則返回 false
     */
    public boolean checkConsistency() {
        lock.readLock().lock();
        try {
            // 檢查兩個 Map 的 key 集合是否相同
            Set<String> counterKeys = profilerCounters.keySet();
            Set<String> timestampKeys = profilerTimestamps.keySet();

            return counterKeys.equals(timestampKeys);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 修復數據一致性問題
     * 移除孤立的數據項
     * 
     * @return 修復的項目數量
     */
    public int repairConsistency() {
        lock.writeLock().lock();
        try {
            int repairedCount = 0;

            // 移除沒有對應 timestamp 的 counter
            Set<String> counterKeysToRemove = profilerCounters.keySet().stream()
                    .filter(key -> !profilerTimestamps.containsKey(key))
                    .collect(Collectors.toSet());

            for (String key : counterKeysToRemove) {
                profilerCounters.remove(key);
                repairedCount++;
            }

            // 移除沒有對應 counter 的 timestamp
            Set<String> timestampKeysToRemove = profilerTimestamps.keySet().stream()
                    .filter(key -> !profilerCounters.containsKey(key))
                    .collect(Collectors.toSet());

            for (String key : timestampKeysToRemove) {
                profilerTimestamps.remove(key);
                repairedCount++;
            }

            return repairedCount;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
