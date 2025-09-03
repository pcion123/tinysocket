package com.vscodelife.serversocket.component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.connection.IConnection;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.message.base.ProtocolReg;
import com.vscodelife.socketio.util.ProtocolScannerUtil;

/**
 * 協議註冊和處理工具類
 * 負責掃描帶有 @ProtocolTag 註解的方法，創建方法處理器，並管理協議註冊
 * 
 * @param <H> Header 型別，必須繼承 HeaderBase
 * @param <C> Connection 型別，必須實現 IConnection
 * @param <M> Message 型別，必須繼承 MessageBase
 * @param <B> Buffer 型別，用於數據傳輸
 */
public class ProtocolRegister<H extends HeaderBase, C extends IConnection<B>, M extends MessageBase<H, B>, B> {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolRegister.class);

    private final Map<ProtocolKey, Consumer<M>> processMap = new ConcurrentHashMap<>();
    private final CacheBase<M, B> cacheManager;
    private final ProtocolScannerUtil.ScanConfig<M> scanConfig;

    /**
     * 連接提供者接口
     */
    @FunctionalInterface
    public interface ConnectionProvider<H extends HeaderBase, C extends IConnection<B>, B> {
        C getConnection(long sessionId);
    }

    /**
     * 異常處理器接口
     */
    @FunctionalInterface
    public interface ExceptionHandler<M> {
        Consumer<M> catchException(Consumer<M> handler);
    }

    /**
     * 構造函數
     * 
     * @param cacheManager       快取管理器
     * @param connectionProvider 連接提供者
     * @param exceptionHandler   異常處理器
     */
    public ProtocolRegister(CacheBase<M, B> cacheManager,
            ConnectionProvider<H, C, B> connectionProvider,
            ExceptionHandler<M> exceptionHandler) {
        this.cacheManager = cacheManager;

        // 建立掃描配置
        this.scanConfig = ProtocolScannerUtil.createServerScanConfig(
                sessionId -> connectionProvider != null ? connectionProvider.getConnection(sessionId) : null,
                exceptionHandler != null ? exceptionHandler::catchException : null,
                cacheManager != null // 根據是否有快取管理器決定是否支援快取
        );
    }

    /**
     * 掃描類中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 
     * @param clazz 要掃描的類
     * @return 掃描到的 ProtocolReg 列表
     */
    public List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz) {
        return ProtocolScannerUtil.scanClassToRegs(clazz, scanConfig);
    }

    /**
     * 掃描類中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 
     * @param clazz         要掃描的類
     * @param withException 是否自動包含 catchException 功能
     * @return 掃描到的 ProtocolReg 列表
     */
    public List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz, boolean withException) {
        return ProtocolScannerUtil.scanClassToRegs(clazz, scanConfig, withException);
    }

    /**
     * 掃描類並自動註冊協議處理器
     * 
     * @param clazz 要掃描的類
     * @return 成功註冊的協議數量
     */
    public int scanAndRegisterProtocols(Class<?> clazz) {
        List<ProtocolReg<M>> protocols = scanClassToRegs(clazz);
        int registeredCount = 0;

        for (ProtocolReg<M> protocol : protocols) {
            try {
                registerProtocol(protocol.getKey(), protocol.getHandler(), protocol.isCached());
                registeredCount++;
            } catch (Exception e) {
                logger.error("Failed to register protocol {}: {}", protocol.getKey(), e.getMessage(), e);
            }
        }

        logger.info("Scanned class {} and registered {} protocols", clazz.getName(), registeredCount);
        return registeredCount;
    }

    /**
     * 註冊協議處理器
     * 
     * @param mainNo  主協議號
     * @param subNo   子協議號
     * @param handler 處理器
     */
    public void registerProtocol(int mainNo, int subNo, Consumer<M> handler) {
        registerProtocol(new ProtocolKey(mainNo, subNo), handler, false);
    }

    public void registerProtocol(int mainNo, int subNo, Consumer<M> handler, boolean cached) {
        registerProtocol(new ProtocolKey(mainNo, subNo), handler, cached);
    }

    public void registerProtocol(ProtocolKey key, Consumer<M> handler) {
        registerProtocol(key, handler, false);
    }

    public void registerProtocol(ProtocolKey key, Consumer<M> handler, boolean cached) {
        processMap.put(key, handler);
        if (cached && cacheManager != null) {
            cacheManager.registerProtocolKey(key);
        }
        logger.debug("Registered protocol {}-{}", key.getMainNo(), key.getSubNo());
    }

    /**
     * 檢查指定協定是否已註冊
     */
    public boolean isProtocolRegistered(int mainNo, int subNo) {
        ProtocolKey key = new ProtocolKey(mainNo, subNo);
        return processMap.containsKey(key);
    }

    public boolean isProtocolRegistered(ProtocolKey key) {
        return processMap.containsKey(key);
    }

    /**
     * 獲取已註冊協定的數量
     */
    public int getRegisteredProtocolCount() {
        return processMap.size();
    }

    /**
     * 清除所有已註冊的協定
     */
    public void clearAllProtocols() {
        processMap.clear();
    }

    /**
     * 獲取協議處理器
     */
    public Consumer<M> getProtocolHandler(ProtocolKey key) {
        return processMap.get(key);
    }

    public Consumer<M> getProtocolHandler(int mainNo, int subNo) {
        return getProtocolHandler(new ProtocolKey(mainNo, subNo));
    }

    /**
     * 獲取所有已註冊的協議鍵
     */
    public java.util.Set<ProtocolKey> getAllProtocolKeys() {
        return processMap.keySet();
    }

}
