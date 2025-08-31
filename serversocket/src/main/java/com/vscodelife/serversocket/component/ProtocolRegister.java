package com.vscodelife.serversocket.component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.annotation.ProtocolTag;
import com.vscodelife.socketio.connection.IConnection;
import com.vscodelife.socketio.message.base.CacheBase;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.message.base.ProtocolReg;

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
    private final ConnectionProvider<H, C, B> connectionProvider;
    private final ExceptionHandler<M> exceptionHandler;

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
        this.connectionProvider = connectionProvider;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * 準備方法調用參數
     * 
     * @param method     目標方法
     * @param paramTypes 參數類型數組
     * @param message    消息對象
     * @return 準備好的參數數組
     */
    private Object[] prepareMethodArguments(Method method, Class<?>[] paramTypes, M message) {
        int paramCount = paramTypes.length;

        if (paramCount == 0) {
            return new Object[0];
        } else if (paramCount == 1) {
            Class<?> paramType = paramTypes[0];

            // 檢查參數類型並提供適當的對象
            if (MessageBase.class.isAssignableFrom(paramType)) {
                // 消息類型參數
                return new Object[] { message };
            } else if (IConnection.class.isAssignableFrom(paramType)) {
                // Connection 類型參數
                H header = message.getHeader();
                if (header != null && connectionProvider != null) {
                    C connection = connectionProvider.getConnection(header.getSessionId());
                    return new Object[] { connection };
                } else {
                    logger.warn(
                            "Message header is null or connection provider is null, cannot provide connection for method {}",
                            method.getName());
                    return new Object[] { null };
                }
            } else if (HeaderBase.class.isAssignableFrom(paramType)) {
                // Header 類型參數
                return new Object[] { message.getHeader() };
            } else {
                // 嘗試傳入消息對象（向後兼容）
                logger.debug("Unknown parameter type {} for method {}, passing message object",
                        paramType.getSimpleName(), method.getName());
                return new Object[] { message };
            }
        } else if (paramCount == 2) {
            // 雙參數方法（消息 + 連接）
            H header = message.getHeader();
            C connection = (header != null && connectionProvider != null)
                    ? connectionProvider.getConnection(header.getSessionId())
                    : null;
            return new Object[] { message, connection };
        } else {
            logger.warn("Unsupported parameter count {} for method {}", paramCount, method.getName());
            return new Object[0];
        }
    }

    /**
     * 為方法創建處理器
     * 
     * @param method 要包裝的方法
     * @param clazz  方法所在的類
     * @return 方法處理器
     */
    private Consumer<M> createMethodHandler(Method method, Class<?> clazz) {
        if (method == null || clazz == null) {
            throw new IllegalArgumentException("Method and class cannot be null");
        }

        // 預先檢查方法參數
        Class<?>[] paramTypes = method.getParameterTypes();
        boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());

        // 為非靜態方法預創建實例以提高效率（如果可能）
        Object instance = null;
        if (!isStatic) {
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.warn("Cannot pre-create instance for class {}, will create on each call: {}",
                        clazz.getName(), e.getMessage());
            }
        }

        final Object finalInstance = instance;

        return (M message) -> {
            try {
                // 確保方法可訪問
                method.setAccessible(true);

                Object[] args = prepareMethodArguments(method, paramTypes, message);
                Object targetInstance = finalInstance;

                // 如果沒有預創建實例，現在創建
                if (!isStatic && targetInstance == null) {
                    try {
                        targetInstance = clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.error("Failed to create instance for method {}.{}: {}",
                                clazz.getName(), method.getName(), e.getMessage());
                        return;
                    }
                }

                // 調用方法
                if (isStatic) {
                    method.invoke(null, args);
                } else {
                    method.invoke(targetInstance, args);
                }

            } catch (IllegalAccessException e) {
                logger.error("Cannot access method {}.{}: {}", clazz.getName(), method.getName(), e.getMessage());
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                logger.error("Method {}.{} threw exception: {}",
                        clazz.getName(), method.getName(),
                        cause != null ? cause.getMessage() : e.getMessage(),
                        cause != null ? cause : e);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid arguments for method {}.{}: {}",
                        clazz.getName(), method.getName(), e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error executing method {}.{}: {}",
                        clazz.getName(), method.getName(), e.getMessage(), e);
            }
        };
    }

    /**
     * 創建帶實例緩存的方法處理器（用於頻繁調用的方法）
     * 
     * @param method   要包裝的方法
     * @param instance 預創建的實例
     * @return 方法處理器
     */
    private Consumer<M> createMethodHandlerWithInstance(Method method, Object instance) {
        return createMethodHandlerWithInstance(method, instance, false);
    }

    /**
     * 創建帶實例緩存和異常處理的方法處理器
     * 
     * @param method   要包裝的方法
     * @param instance 預創建的實例
     * @return 帶有異常處理的方法處理器
     */
    private Consumer<M> createSafeMethodHandlerWithInstance(Method method, Object instance) {
        return createMethodHandlerWithInstance(method, instance, true);
    }

    /**
     * 創建帶實例緩存的方法處理器（用於頻繁調用的方法）
     * 
     * @param method        要包裝的方法
     * @param instance      預創建的實例
     * @param withException 是否自動包含 catchException 功能
     * @return 方法處理器
     */
    private Consumer<M> createMethodHandlerWithInstance(Method method, Object instance, boolean withException) {
        if (method == null) {
            throw new IllegalArgumentException("Method cannot be null");
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());

        if (!isStatic && instance == null) {
            logger.warn("Non-static method {} requires an instance", method.getName());
        }

        Consumer<M> baseHandler = (M message) -> {
            try {
                method.setAccessible(true);
                Object[] args = prepareMethodArguments(method, paramTypes, message);

                if (isStatic) {
                    method.invoke(null, args);
                } else {
                    method.invoke(instance, args);
                }

            } catch (Exception e) {
                logger.error("Error executing method {} with cached instance: {}",
                        method.getName(), e.getMessage(), e);
            }
        };

        // 如果需要包含異常處理，則使用 catchException 包裝
        if (withException) {
            return exceptionHandler.catchException(baseHandler);
        } else {
            return baseHandler;
        }
    }

    /**
     * 為方法創建處理器（自動包含 catchException 功能）
     * 
     * @param method 要包裝的方法
     * @param clazz  方法所在的類
     * @return 帶有異常處理的方法處理器
     */
    private Consumer<M> createMethodHandlerWithCatchException(Method method, Class<?> clazz) {
        Consumer<M> baseHandler = createMethodHandler(method, clazz);
        return exceptionHandler.catchException(baseHandler);
    }

    /**
     * 掃描類中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 
     * @param clazz 要掃描的類
     * @return 掃描到的 ProtocolReg 列表
     */
    public List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz) {
        return scanClassToRegs(clazz, true);
    }

    /**
     * 掃描類中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 
     * @param clazz         要掃描的類
     * @param withException 是否自動包含 catchException 功能
     * @return 掃描到的 ProtocolReg 列表
     */
    public List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz, boolean withException) {
        List<ProtocolReg<M>> result = new ArrayList<>();

        if (clazz == null) {
            logger.warn("Cannot scan null class");
            return result;
        }

        try {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(ProtocolTag.class)) {
                    ProtocolTag tag = method.getAnnotation(ProtocolTag.class);

                    // 創建 ProtocolKey
                    ProtocolKey key = new ProtocolKey(tag.mainNo(), tag.subNo());

                    // 檢查是否已經註冊了相同的協議
                    if (processMap.containsKey(key)) {
                        logger.warn("Protocol {} already registered, skipping method {}.{}",
                                key, clazz.getName(), method.getName());
                        continue;
                    }

                    // 根據註解的 safe 屬性和 withException 參數決定是否使用 catchException
                    boolean useSafe = withException && tag.safed();
                    Consumer<M> handler;
                    if (useSafe) {
                        handler = createMethodHandlerWithCatchException(method, clazz);
                    } else {
                        handler = createMethodHandler(method, clazz);
                    }

                    // 創建 ProtocolReg
                    ProtocolReg<M> protocolReg = new ProtocolReg<>(key, handler, tag.cached());
                    result.add(protocolReg);

                    logger.info("register: {} -> {}.{} (cached: {}, description: {}, safe: {})",
                            key, clazz.getName(), method.getName(), tag.cached(),
                            tag.describe().isEmpty() ? "none" : tag.describe(), useSafe);
                }
            }
        } catch (SecurityException e) {
            logger.error("Security error scanning class {}: {}", clazz.getName(), e.getMessage());
        } catch (NoClassDefFoundError e) {
            logger.warn("skip class {} (dependency is missing)", clazz.getName());
        } catch (Exception e) {
            logger.error("scan class {} error: {}", clazz.getName(), e.getMessage(), e);
        }

        return result;
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
