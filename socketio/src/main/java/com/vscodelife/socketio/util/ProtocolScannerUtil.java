package com.vscodelife.socketio.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vscodelife.socketio.annotation.ProtocolTag;
import com.vscodelife.socketio.message.base.HeaderBase;
import com.vscodelife.socketio.message.base.MessageBase;
import com.vscodelife.socketio.message.base.ProtocolKey;
import com.vscodelife.socketio.message.base.ProtocolReg;

/**
 * 協議掃描工具類
 * 提供通用的協議掃描和方法處理器創建功能
 * 
 * @author VSCodeLife
 * @since 1.0
 */
public class ProtocolScannerUtil {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolScannerUtil.class);

    /**
     * 方法參數準備器介面
     * 負責根據方法簽名和訊息物件準備方法調用參數
     * 
     * @param <M> 訊息類型
     */
    @FunctionalInterface
    public interface ArgumentPreparer<M> {
        /**
         * 準備方法調用參數
         * 
         * @param method     目標方法
         * @param paramTypes 參數類型陣列
         * @param message    訊息物件
         * @return 準備好的參數陣列
         */
        Object[] prepareArguments(Method method, Class<?>[] paramTypes, M message);
    }

    /**
     * 異常處理器介面
     * 
     * @param <M> 訊息類型
     */
    @FunctionalInterface
    public interface ExceptionHandler<M> {
        /**
         * 包裝處理器以提供異常處理
         * 
         * @param handler 原始處理器
         * @return 包裝後的處理器
         */
        Consumer<M> catchException(Consumer<M> handler);
    }

    /**
     * 掃描配置類
     * 包含掃描過程中所需的所有配置參數
     * 
     * @param <M> 訊息類型
     */
    public static class ScanConfig<M> {
        private final ArgumentPreparer<M> argumentPreparer;
        private final ExceptionHandler<M> exceptionHandler;
        private final boolean supportCached;
        private final boolean defaultWithException;

        /**
         * 建構函數
         * 
         * @param argumentPreparer     參數準備器
         * @param exceptionHandler     異常處理器
         * @param supportCached        是否支援快取功能
         * @param defaultWithException 預設是否啟用異常處理
         */
        public ScanConfig(ArgumentPreparer<M> argumentPreparer,
                ExceptionHandler<M> exceptionHandler,
                boolean supportCached,
                boolean defaultWithException) {
            this.argumentPreparer = argumentPreparer;
            this.exceptionHandler = exceptionHandler;
            this.supportCached = supportCached;
            this.defaultWithException = defaultWithException;
        }

        public ArgumentPreparer<M> getArgumentPreparer() {
            return argumentPreparer;
        }

        public ExceptionHandler<M> getExceptionHandler() {
            return exceptionHandler;
        }

        public boolean isSupportCached() {
            return supportCached;
        }

        public boolean isDefaultWithException() {
            return defaultWithException;
        }
    }

    /**
     * 創建基本方法處理器
     * 
     * @param <M>              訊息類型
     * @param method           要包裝的方法
     * @param clazz            方法所在的類別
     * @param argumentPreparer 參數準備器
     * @return 方法處理器
     */
    public static <M> Consumer<M> createMethodHandler(Method method, Class<?> clazz,
            ArgumentPreparer<M> argumentPreparer) {
        if (method == null || clazz == null) {
            throw new IllegalArgumentException("Method and class cannot be null");
        }

        if (argumentPreparer == null) {
            throw new IllegalArgumentException("ArgumentPreparer cannot be null");
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());

        // 為非靜態方法預創建實例以提高效率
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
                method.setAccessible(true);
                Object[] args = argumentPreparer.prepareArguments(method, paramTypes, message);
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
     * 掃描類別中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 使用預設的異常處理設定
     * 
     * @param <M>    訊息類型
     * @param clazz  要掃描的類別
     * @param config 掃描配置
     * @return 掃描到的 ProtocolReg 清單
     */
    public static <M extends MessageBase<? extends HeaderBase, ?>> List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz,
            ScanConfig<M> config) {
        return scanClassToRegs(clazz, config, config.isDefaultWithException());
    }

    /**
     * 掃描類別中帶有 ProtocolTag 註解的方法並轉換為 ProtocolReg
     * 
     * @param <M>           訊息類型
     * @param clazz         要掃描的類別
     * @param config        掃描配置
     * @param withException 是否自動包含 catchException 功能
     * @return 掃描到的 ProtocolReg 清單
     */
    public static <M extends MessageBase<? extends HeaderBase, ?>> List<ProtocolReg<M>> scanClassToRegs(Class<?> clazz,
            ScanConfig<M> config, boolean withException) {
        List<ProtocolReg<M>> result = new ArrayList<>();

        if (clazz == null) {
            logger.warn("Cannot scan null class");
            return result;
        }

        if (config == null) {
            logger.warn("Cannot scan with null config");
            return result;
        }

        try {
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(ProtocolTag.class)) {
                    ProtocolTag tag = method.getAnnotation(ProtocolTag.class);
                    ProtocolKey key = new ProtocolKey(tag.mainNo(), tag.subNo());

                    // 創建方法處理器
                    boolean useSafe = withException && tag.safed();
                    Consumer<M> handler = createMethodHandler(method, clazz, config.getArgumentPreparer());

                    // 如果需要異常處理且有異常處理器，則包裝處理器
                    if (useSafe && config.getExceptionHandler() != null) {
                        handler = config.getExceptionHandler().catchException(handler);
                    }

                    // 決定是否支援快取
                    boolean cached = config.isSupportCached() && tag.cached();

                    // 創建 ProtocolReg
                    ProtocolReg<M> protocolReg = new ProtocolReg<>(key, handler, cached);
                    result.add(protocolReg);

                    // 記錄註冊資訊
                    String cacheInfo = config.isSupportCached() ? String.format("cached: %s, ", cached) : "";
                    logger.info("register: {} -> {}.{} ({}description: {}, safe: {})",
                            key, clazz.getName(), method.getName(), cacheInfo,
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
     * 為客戶端創建參數準備器
     * 客戶端參數準備器的特點：
     * - 支援 MessageBase 類型參數
     * - 支援 HeaderBase 類型參數
     * - 不支援 Connection 類型參數
     * 
     * @param <H> Header 類型
     * @param <M> Message 類型
     * @param <B> Buffer 類型
     * @return 客戶端參數準備器
     */
    public static <H extends HeaderBase, M extends MessageBase<H, B>, B> ArgumentPreparer<M> createClientArgumentPreparer() {
        return (method, paramTypes, message) -> {
            int paramCount = paramTypes.length;

            if (paramCount == 0) {
                return new Object[0];
            } else if (paramCount == 1) {
                Class<?> paramType = paramTypes[0];

                if (MessageBase.class.isAssignableFrom(paramType)) {
                    // 訊息類型參數
                    return new Object[] { message };
                } else if (HeaderBase.class.isAssignableFrom(paramType)) {
                    // Header 類型參數
                    return new Object[] { message.getHeader() };
                } else {
                    // 嘗試傳入訊息物件（向後相容）
                    logger.debug("Unknown parameter type {} for method {}, passing message object",
                            paramType.getSimpleName(), method.getName());
                    return new Object[] { message };
                }
            } else {
                logger.warn("Unsupported parameter count {} for method {}", paramCount, method.getName());
                return new Object[0];
            }
        };
    }

    /**
     * 創建簡單的異常處理器
     * 這個異常處理器會捕獲所有異常並記錄，但不會重新拋出
     * 
     * @param <M> 訊息類型
     * @return 簡單異常處理器
     */
    public static <M> ExceptionHandler<M> createSimpleExceptionHandler() {
        return (handler) -> (message) -> {
            try {
                handler.accept(message);
            } catch (Exception e) {
                logger.error("Exception caught in protocol handler: {}", e.getMessage(), e);
            }
        };
    }

    /**
     * 為伺服器端創建參數準備器
     * 伺服器端參數準備器的特點：
     * - 支援 MessageBase 類型參數
     * - 支援 HeaderBase 類型參數
     * - 支援 IConnection 類型參數
     * - 支援雙參數方法（訊息 + 連接）
     * 
     * @param <H>                Header 類型
     * @param <C>                Connection 類型
     * @param <M>                Message 類型
     * @param <B>                Buffer 類型
     * @param connectionProvider 連接提供者
     * @return 伺服器端參數準備器
     */
    public static <H extends HeaderBase, C, M extends MessageBase<H, B>, B> ArgumentPreparer<M> createServerArgumentPreparer(
            java.util.function.Function<Long, C> connectionProvider) {
        return (method, paramTypes, message) -> {
            int paramCount = paramTypes.length;

            if (paramCount == 0) {
                return new Object[0];
            } else if (paramCount == 1) {
                Class<?> paramType = paramTypes[0];

                if (MessageBase.class.isAssignableFrom(paramType)) {
                    // 訊息類型參數
                    return new Object[] { message };
                } else if (HeaderBase.class.isAssignableFrom(paramType)) {
                    // Header 類型參數
                    return new Object[] { message.getHeader() };
                } else if (com.vscodelife.socketio.connection.IConnection.class.isAssignableFrom(paramType)) {
                    // 連接類型參數
                    H header = message.getHeader();
                    C connection = (header != null && connectionProvider != null)
                            ? connectionProvider.apply(header.getSessionId())
                            : null;
                    return new Object[] { connection };
                } else {
                    // 嘗試傳入訊息物件（向後相容）
                    logger.debug("Unknown parameter type {} for method {}, passing message object",
                            paramType.getSimpleName(), method.getName());
                    return new Object[] { message };
                }
            } else if (paramCount == 2) {
                // 雙參數方法（訊息 + 連接）
                H header = message.getHeader();
                C connection = (header != null && connectionProvider != null)
                        ? connectionProvider.apply(header.getSessionId())
                        : null;
                return new Object[] { message, connection };
            } else {
                logger.warn("Unsupported parameter count {} for method {}", paramCount, method.getName());
                return new Object[0];
            }
        };
    }

    /**
     * 創建客戶端掃描配置
     * 
     * @param <H>              Header 類型
     * @param <M>              Message 類型
     * @param <B>              Buffer 類型
     * @param exceptionHandler 異常處理器（可為 null）
     * @return 客戶端掃描配置
     */
    public static <H extends HeaderBase, M extends MessageBase<H, B>, B> ScanConfig<M> createClientScanConfig(
            ExceptionHandler<M> exceptionHandler) {
        return new ScanConfig<>(
                createClientArgumentPreparer(),
                exceptionHandler,
                false, // 客戶端不支援快取
                true // 預設啟用異常處理
        );
    }

    /**
     * 創建伺服器端掃描配置
     * 
     * @param <H>                Header 類型
     * @param <C>                Connection 類型
     * @param <M>                Message 類型
     * @param <B>                Buffer 類型
     * @param connectionProvider 連接提供者
     * @param exceptionHandler   異常處理器（可為 null）
     * @param supportCached      是否支援快取功能
     * @return 伺服器端掃描配置
     */
    public static <H extends HeaderBase, C, M extends MessageBase<H, B>, B> ScanConfig<M> createServerScanConfig(
            java.util.function.Function<Long, C> connectionProvider,
            ExceptionHandler<M> exceptionHandler,
            boolean supportCached) {
        return new ScanConfig<>(
                createServerArgumentPreparer(connectionProvider),
                exceptionHandler,
                supportCached, // 伺服器端支援快取
                true // 預設啟用異常處理
        );
    }

    /**
     * 快速掃描類別（使用預設客戶端配置）
     * 
     * @param <H>   Header 類型
     * @param <M>   Message 類型
     * @param <B>   Buffer 類型
     * @param clazz 要掃描的類別
     * @return 掃描到的 ProtocolReg 清單
     */
    public static <H extends HeaderBase, M extends MessageBase<H, B>, B> List<ProtocolReg<M>> quickScanForClient(
            Class<?> clazz) {
        ScanConfig<M> config = createClientScanConfig(createSimpleExceptionHandler());
        return scanClassToRegs(clazz, config);
    }
}
