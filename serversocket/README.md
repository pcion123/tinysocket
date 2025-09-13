# ServerSocket 服務器端Socket模組

ServerSocket 是 TinySocket 專案的服務器端 Socket 實現模組，基於 socketio 核心庫構建的高性能、高併發 Socket 服務器框架。它提供完整的泛型設計架構，支援二進制和 JSON 兩種通信協議，內建豐富的組件系統，為企業級應用提供生產就緒的 Socket 服務器解決方案。

## 📋 模組概述

ServerSocket 模組實現了 TinySocket 框架的服務器端核心功能，包括：

- **🚀 高性能 Socket 服務器**: 基於 Netty 4.1.115 的異步 I/O 架構
- **🔧 泛型設計架構**: 完整的泛型約束確保類型安全
- **📨 多協議支援**: ByteSocket（二進制）和 JsonSocket（JSON，含WebSocket支援）
- **⚙️ 組件化系統**: 限流器、協議處理器、連接管理等可插拔組件
- **🔍 註解驅動開發**: 使用 @ProtocolTag 自動註冊協議處理器
- **💬 實戰應用**: 包含完整的聊天服務器實現示例

### 🎯 設計理念

- **高性能**: 基於 Netty NIO，支援高並發場景
- **類型安全**: 完整的泛型設計和編譯期檢查
- **組件化**: 可插拔的組件架構，易於擴展和維護
- **生產就緒**: 內建限流、監控、異常處理等企業級特性
- **開發友好**: 註解驅動開發，減少樣板代碼

## 🏗️ 架構設計

### 核心組件架構

![ServerSocket 架構設計](assets/serversocket-architecture.svg)

**架構層次圖說明**: 上圖展示了 ServerSocket 的分層架構設計，從應用層到核心層的完整技術棧。

### 詳細組件結構

```
serversocket/
├── src/main/java/com/vscodelife/serversocket/
│   ├── SocketBase.java                # Socket 服務器基類（泛型設計）
│   │   ├── 泛型約束: <H, C, M, B>
│   │   ├── 連接管理: connectionMap
│   │   ├── 訊息處理: messageQueue
│   │   ├── 協議註冊: protocolRegister
│   │   └── 組件系統: rateLimiter, protocolCatcher
│   ├── ByteSocket.java                # 二進制 Socket 服務器（抽象類）
│   │   ├── 繼承: SocketBase<H, C, ByteMessage<H, ByteArrayBuffer>, ByteArrayBuffer>
│   │   ├── 泛型約束: <H extends HeaderBase, C extends IConnection<ByteArrayBuffer>>
│   │   ├── 快取管理: ByteCache
│   │   └── 連接管理: ByteArrayBuffer 數據處理
│   ├── JsonSocket.java                # JSON Socket 服務器（抽象類）
│   │   ├── 繼承: SocketBase<H, C, JsonMessage<H, JsonMapBuffer>, JsonMapBuffer>
│   │   ├── 泛型約束: <H extends HeaderBase, C extends IConnection<JsonMapBuffer>>
│   │   ├── 快取管理: JsonCache
│   │   └── 連接管理: JsonMapBuffer 數據處理
│   ├── component/                     # 組件系統
│   │   ├── RateLimiter.java               # 限流器組件
│   │   │   ├── 隨機過濾限流算法
│   │   │   ├── 時間窗口限流控制
│   │   │   └── 動態開關控制機制
│   │   ├── ProtocolCatcher.java           # 協議異常捕獲器
│   │   │   ├── 異常處理包裝
│   │   │   ├── 錯誤日誌記錄
│   │   │   └── 優雅降級處理
│   │   └── ProtocolRegister.java          # 協議註冊器
│   │       ├── 自動協議掃描
│   │       ├── @ProtocolTag 註解處理
│   │       └── 協議方法映射
│   └── connection/                    # 連接管理實現
│       ├── ByteConnection.java            # 二進制連接實現（抽象類）
│       │   ├── 實現: IConnection<ByteArrayBuffer>
│       │   ├── 二進制數據處理
│       │   ├── 連接狀態管理
│       │   └── 會話生命周期處理
│       └── JsonConnection.java            # JSON 連接實現（抽象類）
│           ├── 實現: IConnection<JsonMapBuffer>
│           ├── JSON 數據處理
│           ├── 連接狀態管理
│           └── 會話生命周期處理
```

### 架構層次說明

ServerSocket 採用分層架構設計，從上到下分為四個層次：

1. **Application Layer（應用層）**
   - 用戶自定義的 Socket 服務器實現
   - 繼承 ByteSocket 或 JsonSocket 進行業務開發
   - 如遊戲服務器、聊天服務器等

2. **ServerSocket Framework（框架層）**
   - ByteSocket: 二進制數據傳輸服務器
   - JsonSocket: JSON 數據傳輸服務器
   - ByteConnection/JsonConnection: 連接管理實現
   - SocketBase: 泛型基類，提供完整的類型約束

3. **Component Layer（組件層）**
   - RateLimiter: 限流器，支援隨機過濾和時間窗口限流
   - ProtocolCatcher: 協議異常捕獲和處理
   - ProtocolRegister: 協議註冊器，支援 @ProtocolTag 註解
   - CacheManager: 快取管理器

4. **SocketIO Core（核心層）**
   - 基於 Netty 的高性能網絡通信
   - ByteArrayBuffer, JsonUtil, ProfilerUtil, SnowflakeUtil 等核心類
   - FastJSON 序列化支援

### 泛型設計架構

ServerSocket 採用完整的泛型設計，確保類型安全：

```java
public abstract class SocketBase<H extends HeaderBase, 
                                C extends IConnection<B>, 
                                M extends MessageBase<H, B>, 
                                B> implements Runnable
```

**泛型參數說明**：
- `H`: Header 類型，必須繼承 `HeaderBase`
- `C`: Connection 類型，必須實現 `IConnection<B>`
- `M`: Message 類型，必須繼承 `MessageBase<H, B>`
- `B`: Buffer 類型，用於數據傳輸（如 `ByteArrayBuffer` 或 `JsonMapBuffer`）

## 🚀 核心功能

### 1. SocketBase 泛型基類設計

SocketBase 是所有 Socket 服務器的基類，提供完整的泛型約束：

```java
public class MyCustomSocket extends SocketBase<CustomHeader, CustomConnection, CustomMessage, ByteArrayBuffer> {
    
    public MyCustomSocket(int port, int limitConnect) {
        super(logger, port, limitConnect, MyInitializer.class);
        
        // 註解驅動協議註冊
        int protocolCount = protocolRegister.scanAndRegisterProtocols(MyProtocol.class);
        logger.info("註冊協議數量: {}", protocolCount);
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    protected Class<CustomConnection> getConnectionClass() {
        return CustomConnection.class;
    }
    
    @Override
    public void onConnect(long sessionId) {
        logger.info("客戶端連接: sessionId={}", sessionId);
        
        // 獲取連接對象
        CustomConnection connection = getConnection(sessionId);
        if (connection != null) {
            connection.setConnectTime(new Date());
        }
    }
    
    @Override
    public void onDisconnect(long sessionId) {
        logger.info("客戶端斷開: sessionId={}", sessionId);
        
        // 清理連接相關資源
        cleanupConnection(sessionId);
    }
}
```

### 2. ByteSocket 二進制服務器

ByteSocket 專為高性能二進制數據傳輸設計：

```java
public class GameServer extends ByteSocket<GameHeader, GameConnection> {
    
    public GameServer(int port, int maxConnections) {
        super(logger, port, maxConnections, GameInitializer.class);
        
        // 自動掃描並註冊協議處理器
        protocolRegister.scanAndRegisterProtocols(GameProtocol.class);
    }
    
    @Override
    protected Class<GameConnection> getConnectionClass() {
        return GameConnection.class;
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public void onConnect(long sessionId) {
        super.onConnect(sessionId);
        
        GameConnection connection = getConnection(sessionId);
        if (connection != null) {
            connection.setGameState(GameState.LOBBY);
            
            // 發送歡迎訊息
            ByteArrayBuffer welcome = new ByteArrayBuffer();
            welcome.writeString("歡迎來到遊戲服務器！");
            send(sessionId, GameProtocol.WELCOME, 0, welcome);
        }
    }
    
    @Override
    public void onDisconnect(long sessionId) {
        GameConnection connection = getConnection(sessionId);
        if (connection != null && connection.isInGame()) {
            // 處理遊戲中斷開邏輯
            handlePlayerLeaveGame(sessionId, connection);
        }
        super.onDisconnect(sessionId);
    }
}

// 協議處理器類
public final class GameProtocol {
    public static final int WELCOME = 1;
    public static final int LOGIN = 2;
    public static final int JOIN_GAME = 3;
    public static final int MOVE_PLAYER = 4;
    
    public static GameServer server;
    
    @ProtocolTag(mainNo = 1, subNo = 1, cached = false, safed = true, describe = "玩家登入")
    public static void handleLogin(ByteMessage<GameHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        String username = message.getBuffer().readString();
        String password = message.getBuffer().readString();
        
        // 驗證用戶
        if (validateUser(username, password)) {
            GameConnection connection = server.getConnection(sessionId);
            connection.setUsername(username);
            connection.setGameState(GameState.AUTHENTICATED);
            
            // 回應登入成功
            ByteArrayBuffer response = new ByteArrayBuffer();
            response.writeInt(1); // 成功
            response.writeString("登入成功");
            server.send(sessionId, LOGIN, message.getHeader().getRequestId(), response);
        }
    }
    
    @ProtocolTag(mainNo = 1, subNo = 3, cached = true, safed = false, describe = "加入遊戲")
    public static void handleJoinGame(ByteMessage<GameHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        int gameRoomId = message.getBuffer().readInt();
        
        // 加入遊戲房間邏輯
        GameConnection connection = server.getConnection(sessionId);
        if (connection.isAuthenticated()) {
            joinGameRoom(sessionId, gameRoomId);
        }
    }
    
    @ProtocolTag(mainNo = 2, subNo = 1, cached = false, safed = false, describe = "玩家移動")
    public static void handlePlayerMove(ByteMessage<GameHeader> message) {
        long sessionId = message.getHeader().getSessionId();
        float x = message.getBuffer().readFloat();
        float y = message.getBuffer().readFloat();
        
        // 處理玩家移動
        updatePlayerPosition(sessionId, x, y);
        
        // 廣播給房間內其他玩家
        broadcastToRoom(sessionId, message);
    }
}
```

### 3. JsonSocket JSON 服務器

JsonSocket 提供便於調試和跨語言通信的 JSON 協議支援，特別適用於Web應用和聊天系統：

```java
public class ApiServer extends JsonSocket<ApiHeader, ApiConnection> {
    
    public ApiServer(int port, int maxConnections) {
        super(logger, port, maxConnections, ApiInitializer.class);
        
        // 註冊 JSON 協議處理器
        protocolRegister.scanAndRegisterProtocols(ApiProtocol.class);
    }
    
    @Override
    protected Class<ApiConnection> getConnectionClass() {
        return ApiConnection.class;
    }
    
    @Override
    public String getVersion() {
        return "2.0.0";
    }
}

// JSON 協議處理器
public final class ApiProtocol {
    public static ApiServer server;
    
    @ProtocolTag(mainNo = 1, subNo = 1, describe = "用戶註冊")
    public static void handleUserRegister(JsonMessage<ApiHeader> message) {
        JsonObject data = message.getBuffer();
        
        String username = data.getString("username");
        String email = data.getString("email");
        String password = data.getString("password");
        
        // 處理用戶註冊邏輯
        UserRegistrationResult result = registerUser(username, email, password);
        
        // 構建 JSON 回應
        JsonObject response = new JsonObject();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        if (result.isSuccess()) {
            response.put("userId", result.getUserId());
        }
        
        server.send(message.getHeader().getSessionId(), 1, 1, 
                   message.getHeader().getRequestId(), response);
    }
}
```

### 4. 組件化系統

#### 限流器 (RateLimiter)

```java
public class RateLimiterExample {
    
    public void configureRateLimit() {
        RateLimiter rateLimiter = new RateLimiter();
        
        // 啟用限流：10分鐘限流窗口，20% 過濾率（80% 通過率）
        rateLimiter.enable(10 * 60 * 1000L, 20);
        
        // 檢查請求是否通過限流
        if (!rateLimiter.pass()) {
            // 拒絕請求
            sendErrorResponse("系統繁忙，請稍後再試");
            return;
        }
        
        // 處理正常請求
        processRequest();
        
        // 動態調整限流策略
        if (systemOverloaded()) {
            // 提高過濾率到 50%（50% 通過率）
            rateLimiter.enable(5 * 60 * 1000L, 50);
        }
        
        // 系統恢復時關閉限流
        if (systemRecovered()) {
            rateLimiter.disable();
        }
    }
    
    public void customRateLimitConfig() {
        // 創建自定義限流器
        RateLimiter customLimiter = new RateLimiter(true, 30 * 60 * 1000L, 30);
        
        // 檢查限流狀態
        if (customLimiter.isEnabled()) {
            long remainingTime = customLimiter.getLimitTime() - System.currentTimeMillis();
            int filterRate = customLimiter.getFilterRate();
            
            logger.info("限流中：剩餘時間 {}ms，過濾率 {}%", remainingTime, filterRate);
        }
    }
}
```

#### 協議異常捕獲器 (ProtocolCatcher)

```java
public class ProtocolCatcherExample {
    
    public void setupExceptionHandling() {
        // 使用 catchException 包裝協議處理器
        registerProtocol(1, 1, catchException(this::handleLogin));
        registerProtocol(1, 2, catchException(this::handleLogout));
    }
    
    private void handleLogin(ByteMessage<HeaderBase> message) {
        // 可能拋出異常的業務邏輯
        String username = message.getBuffer().readString();
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("用戶名不能為空");
        }
        
        // 數據庫操作可能拋出異常
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException("用戶不存在");
        }
        
        // 正常處理邏輯...
    }
    
    // catchException 會自動捕獲異常並記錄日誌
    private Consumer<ByteMessage<HeaderBase>> catchException(
            Consumer<ByteMessage<HeaderBase>> handler) {
        return message -> {
            try {
                handler.accept(message);
            } catch (Exception e) {
                logger.error("處理協議時發生異常: mainNo={}, subNo={}, sessionId={}", 
                           message.getHeader().getMainNo(),
                           message.getHeader().getSubNo(),
                           message.getHeader().getSessionId(), e);
                           
                // 發送錯誤回應給客戶端
                sendErrorResponse(message, "服務器內部錯誤");
            }
        };
    }
}
```

### 5. 連接管理系統

#### 自定義連接實現

```java
public class GameConnection implements IConnection<ByteArrayBuffer> {
    private long sessionId;
    private String username;
    private GameState gameState;
    private int gameRoomId;
    private Date connectTime;
    private Date lastActiveTime;
    private String clientIp;
    private String clientVersion;
    
    public GameConnection(long sessionId) {
        this.sessionId = sessionId;
        this.gameState = GameState.CONNECTED;
        this.connectTime = new Date();
        this.lastActiveTime = new Date();
    }
    
    @Override
    public long getSessionId() {
        return sessionId;
    }
    
    @Override
    public void updateLastActiveTime() {
        this.lastActiveTime = new Date();
    }
    
    @Override
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - lastActiveTime.getTime() > timeoutMs;
    }
    
    @Override
    public void release() {
        // 釋放連接相關資源
        if (isInGame()) {
            leaveCurrentGame();
        }
        gameState = GameState.DISCONNECTED;
    }
    
    // 業務相關方法
    public boolean isAuthenticated() {
        return gameState == GameState.AUTHENTICATED || gameState == GameState.IN_GAME;
    }
    
    public boolean isInGame() {
        return gameState == GameState.IN_GAME && gameRoomId > 0;
    }
    
    public void joinGame(int roomId) {
        this.gameRoomId = roomId;
        this.gameState = GameState.IN_GAME;
    }
    
    public void leaveCurrentGame() {
        this.gameRoomId = 0;
        this.gameState = GameState.AUTHENTICATED;
    }
    
    // getter/setter 方法...
}

// 遊戲狀態枚舉
public enum GameState {
    CONNECTED,      // 已連接但未認證
    AUTHENTICATED,  // 已認證但未進入遊戲
    IN_GAME,        // 遊戲中
    DISCONNECTED    // 已斷開
}
```

## 📈 性能特性

- **零拷貝緩衝區**: 減少記憶體分配和 GC 壓力
- **異步 I/O**: 基於 Netty NIO，支援高並發
- **連接池化**: 可重用連接管理，降低開銷
- **訊息快取**: 智能的訊息快取管理
- **限流保護**: 多層級限流防止系統過載

## 🔮 發展計劃

### 已完成功能 ✅
- [x] **SocketBase 泛型基類**: 完整的泛型約束設計
- [x] **ByteSocket/JsonSocket**: 二進制和 JSON 協議支援
- [x] **組件化架構**: 限流器、協議處理器、連接管理
- [x] **註解驅動開發**: @ProtocolTag 自動協議註冊

---

**由 vscodelife 團隊精心打造** ❤️  
*讓高性能 Socket 服務器開發變得簡單而高效*

> **版本**: v0.0.1-SNAPSHOT  
> **最後更新**: 2025年9月13日  
> **Java版本**: OpenJDK 21+  
> **技術棧**: Netty 4.1.115 + Spring Boot 3.5.4 + FastJSON 2.0.52
> **新增功能**: JsonSocket聊天服務器 + 完整業務示例

[![GitHub Stars](https://img.shields.io/github/stars/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Forks](https://img.shields.io/github/forks/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Issues](https://img.shields.io/github/issues/vscodelife/tinysocket)](https://github.com/vscodelife/tinysocket/issues)
[![License](https://img.shields.io/github/license/vscodelife/tinysocket)](../LICENSE)
