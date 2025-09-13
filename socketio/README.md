# SocketIO 核心通信庫

SocketIO 是 TinySocket 專案的核心通信庫，提供完整的高性能網絡通信基礎設施。作為整個框架的基石，它為 TinySocket 生態系統提供統一的通信協議、高性能緩衝區管理、結構化訊息處理、豐富的工具類庫和註解驅動的開發體驗。

## 📋 模組概述

SocketIO 模組實現了 TinySocket 框架的核心功能，包括：

- **🔧 雙緩衝區系統**: `ByteArrayBuffer` 零拷貝二進制緩衝區 + `JsonMapBuffer` JSON 數據緩衝區
- **📨 結構化訊息系統**: 基於註解的自動序列化/反序列化，支援 `@MessageTag` 和 `@ProtocolTag`
- **🛠️ 豐富工具類庫**: JSON 處理、分散式 ID、性能分析、HTTP 客戶端等
- **🔌 連接管理接口**: 通用連接管理接口 `IConnection`
- **⚡ 協議處理系統**: 協議註冊、快取管理和異常處理

### 🎯 設計理念

- **高性能**: 零拷貝緩衝區設計，支援二進制和 JSON 雙重優化
- **類型安全**: 完整的泛型支援和編譯期檢查
- **註解驅動**: 透過註解自動處理序列化和協議註冊
- **模組化**: 清晰的 API 邊界，支援插件式擴展
- **多協議支援**: 同時支援高效二進制和標準 JSON 通信
- **開發友好**: 豐富的工具類和詳細的錯誤信息

**注意**: 本模組命名為 socketio，但與 Node.js 的 Socket.IO 協議無關。它是 TinySocket 專案的自定義通信協議實現，基於 Netty 4.1.115 和 Java 21 構建。

## 🏗️ 架構設計

### 核心組件架構

```
socketio/
├── annotation/                    # 註解系統
│   ├── MessageTag.java           # 序列化欄位標記註解
│   └── ProtocolTag.java          # 協議方法標記註解
├── buffer/                       # 緩衝區管理
│   ├── ByteArrayBuffer.java      # 高性能可重用位元組緩衝區
│   └── JsonMapBuffer.java        # JSON 數據緩衝區
├── connection/                   # 連接管理
│   └── IConnection.java          # 通用連接接口定義
├── constant/                     # 協議常數
│   └── ProtocolId.java           # 內建協議ID常數
├── message/                      # 訊息處理系統
│   ├── base/                     # 基礎訊息類
│   │   ├── HeaderBase.java       # 訊息頭基類
│   │   ├── MessageBase.java      # 訊息基類
│   │   ├── CacheBase.java        # 快取基類
│   │   ├── ProtocolKey.java      # 協議鍵
│   │   └── ProtocolReg.java      # 協議註冊
│   ├── ByteMessage.java          # 二進制訊息實現
│   ├── JsonMessage.java          # JSON 訊息實現
│   ├── ByteCache.java            # 二進制訊息快取
│   └── JsonCache.java            # JSON 訊息快取
└── util/                         # 工具類集合
    ├── Base64Util.java           # Base64 編解碼工具
    ├── DateUtil.java             # 日期時間處理工具
    ├── ExecutorUtil.java         # 線程池管理工具
    ├── JsonUtil.java             # JSON 處理工具（FastJSON2）
    ├── JwtUtil.java              # JWT Token 處理工具
    ├── NettyUtil.java            # Netty 相關工具
    ├── ProtocolScannerUtil.java  # 協議掃描工具
    ├── RandomUtil.java           # 隨機數生成工具
    ├── SnowflakeUtil.java        # 分散式ID生成器
    ├── StrUtil.java              # 字串處理工具
    ├── http/                     # HTTP 相關工具
    │   ├── HttpUtil.java             # HTTP 客戶端工具
    │   └── HttpResponse.java         # HTTP 響應封裝
    └── profiler/                 # 性能分析工具
        ├── ProfilerUtil.java         # 性能分析工具主類
        ├── ProfilerCounter.java      # 性能計數器
        ├── ProfilerConfig.java       # 性能分析配置
        └── ProfilerCounterManager.java # 計數器管理器
```

### 設計模式與架構理念

- **泛型設計**: 使用完整的泛型約束確保類型安全
- **零拷貝**: ByteArrayBuffer 實現零拷貝緩衝區操作
- **註解驅動**: 使用 @MessageTag 和 @ProtocolTag 簡化開發
- **快取管理**: 內建訊息快取和連接池管理
- **性能監控**: 內建 ProfilerUtil 性能分析系統

## 🚀 核心功能

### 1. 高性能緩衝區管理

SocketIO 提供兩種高性能的緩衝區實現，分別適用於不同的通信場景：

#### 1.1 ByteArrayBuffer - 二進制緩衝區

提供類似 Netty ByteBuf 的 API，支援高效的二進制數據操作：

##### 基本操作

```java
import com.vscodelife.socketio.buffer.ByteArrayBuffer;
import com.vscodelife.socketio.buffer.ByteArrayBuffer.ByteOrder;

// 創建緩衝區
ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

// 支援雙字節序
ByteArrayBuffer bigEndian = new ByteArrayBuffer(1024, ByteOrder.BIG_ENDIAN);
ByteArrayBuffer littleEndian = new ByteArrayBuffer(1024, ByteOrder.LITTLE_ENDIAN);

// 基本數據寫入
buffer.writeBoolean(true)
      .writeByte(127)
      .writeShort(32767)
      .writeInt(2147483647)
      .writeLong(9223372036854775807L)
      .writeFloat(3.14f)
      .writeDouble(3.1415926)
      .writeString("TinySocket");

// 基本數據讀取
boolean boolValue = buffer.readBoolean();
byte byteValue = buffer.readByte();
short shortValue = buffer.readShort();
int intValue = buffer.readInt();
long longValue = buffer.readLong();
float floatValue = buffer.readFloat();
double doubleValue = buffer.readDouble();
String stringValue = buffer.readString();
```

##### 進階功能

```java
// 壓縮功能
buffer.compress();    // 使用 GZIP 壓縮
buffer.decompress();  // 解壓縮

// 緩衝區管理
buffer.clear();       // 清空緩衝區重複使用
buffer.reset();       // 重置讀寫指針
byte[] data = buffer.toByteArray(); // 轉換為字節陣列

// 容量管理
buffer.ensureWritable(1024); // 確保可寫空間
int readable = buffer.readableBytes();
int writable = buffer.writableBytes();
```

#### 1.2 JsonMapBuffer - JSON 緩衝區

專為 JSON 數據處理設計的高效緩衝區，基於 FastJSON2 實現：

##### 基本操作

```java
import com.vscodelife.socketio.buffer.JsonMapBuffer;

// 創建 JSON 緩衝區
JsonMapBuffer jsonBuffer = new JsonMapBuffer();

// 從 JSON 字串創建
JsonMapBuffer fromJson = new JsonMapBuffer("{\"name\":\"Alice\",\"age\":25}");

// 數據寫入
jsonBuffer.put("userId", 12345L);
jsonBuffer.put("username", "alice");
jsonBuffer.put("isActive", true);
jsonBuffer.put("score", 98.5);
jsonBuffer.put("tags", Arrays.asList("admin", "user"));

// 基本數據讀取
long userId = jsonBuffer.getLong("userId");
String username = jsonBuffer.getString("username");
boolean isActive = jsonBuffer.getBoolean("isActive");
double score = jsonBuffer.getDouble("score");
Date createTime = jsonBuffer.getDate("createTime");
```

##### 高級數據類型

```java
// 支援各種數據類型
jsonBuffer.put("bigNumber", new BigDecimal("123456789.987654321"));
jsonBuffer.put("timestamp", new Date());
jsonBuffer.put("bytes", "Hello".getBytes());

// 讀取各種數據類型
BigDecimal bigDecimal = jsonBuffer.getBigDecimal("bigNumber");
BigInteger bigInteger = jsonBuffer.getBigInteger("largeNumber");
byte[] bytes = jsonBuffer.getBytes("bytes");
float floatValue = jsonBuffer.getFloat("ratio");
int intValue = jsonBuffer.getInteger("count");
short shortValue = jsonBuffer.getShort("status");
```

##### JSON 序列化與轉換

```java
// 轉換為 JSON 字串
String json = jsonBuffer.toJson();
// 輸出: {"userId":12345,"username":"alice","isActive":true,"score":98.5}

// 重設緩衝區內容
jsonBuffer.setBuffer("{\"newData\":\"value\"}");

// 獲取底層 JSONObject（用於進階操作）
JSONObject jsonObject = jsonBuffer.getBuffer();

// 克隆緩衝區
JsonMapBuffer cloned = jsonBuffer.clone();
```

##### 與其他組件整合

```java
// 與 JsonMessage 配合使用
JsonMessage<HeaderBase> message = new JsonMessage<>();
JsonMapBuffer buffer = new JsonMapBuffer();
buffer.put("action", "login");
buffer.put("credentials", userCredentials);
message.setBuffer(buffer);

// 在協議處理中使用
@ProtocolTag(mainNo = 1, subNo = 1, describe = "處理JSON請求")
public static void handleJsonRequest(JsonMessage<HeaderBase> message) {
    JsonMapBuffer buffer = message.getBuffer();
    String action = buffer.getString("action");
    
    // 創建響應
    JsonMapBuffer response = new JsonMapBuffer();
    response.put("status", "success");
    response.put("timestamp", System.currentTimeMillis());
}
```

#### 1.3 緩衝區選擇指南

| 場景 | 建議緩衝區 | 優勢 |
|------|-----------|------|
| **高性能二進制通信** | ByteArrayBuffer | 零拷貝、緊湊格式、高吞吐量 |
| **Web API / REST服務** | JsonMapBuffer | 人可讀、跨平台、易於調試 |
| **混合數據格式** | 兩者搭配 | 靈活性最大，適應不同需求 |
| **大量小訊息** | ByteArrayBuffer | 減少序列化開銷 |
| **複雜嵌套數據** | JsonMapBuffer | 自然支援嵌套結構 |

### 2. 註解驅動序列化系統

#### @MessageTag 序列化註解

用於標記需要自動序列化的欄位：

```java
import com.vscodelife.socketio.annotation.MessageTag;

public class UserInfo {
    @MessageTag(order = 1)
    private int userId;
    
    @MessageTag(order = 2)
    private String username;
    
    @MessageTag(order = 3)
    private Date loginTime;
    
    @MessageTag(order = 4)
    private List<String> roles;
    
    // 不標記的欄位不會被序列化
    private transient String password;
}

// 自動序列化
buffer.writeStruct(userInfo);
UserInfo received = buffer.readStruct(UserInfo.class);
```

#### @ProtocolTag 協議處理註解

用於標記協議處理方法，支援自動註冊：

```java
import com.vscodelife.socketio.annotation.ProtocolTag;

public class ServerProtocol {
    
    @ProtocolTag(mainNo = 1, subNo = 1, cached = false, safed = true, describe = "用戶登入")
    public static void handleLogin(ByteMessage<HeaderBase> message) {
        // 處理登入邏輯
        String username = message.getBuffer().readString();
        String password = message.getBuffer().readString();
        
        // 業務邏輯處理...
    }
    
    @ProtocolTag(mainNo = 1, subNo = 2, cached = true, safed = false, describe = "用戶登出")
    public static void handleLogout(ByteMessage<HeaderBase> message) {
        // 處理登出邏輯
        long userId = message.getBuffer().readLong();
        
        // 業務邏輯處理...
    }
}
```

### 3. 結構化訊息系統

#### 訊息基礎架構

```java
// 自定義訊息頭
public class CustomHeader extends HeaderBase {
    private String clientVersion;
    private int deviceType;
    private String token;
    
    // 構造方法和 getter/setter...
}

// 二進制訊息
ByteMessage<CustomHeader> byteMessage = new ByteMessage<>(header, buffer);

// JSON 訊息
JsonMessage<CustomHeader> jsonMessage = new JsonMessage<>(header, jsonObject);
```

#### 訊息快取管理

```java
// 二進制訊息快取
ByteCache<CustomHeader> byteCache = new ByteCache<>();

// JSON 訊息快取
JsonCache<CustomHeader> jsonCache = new JsonCache<>();

// 快取操作
CustomMessage message = cache.get();     // 獲取可重用訊息對象
cache.release(message);                  // 釋放訊息對象回快取
```

### 4. 豐富的工具類庫

#### JSON 高性能處理

```java
import com.vscodelife.socketio.util.JsonUtil;

// 序列化（支援 null 值）
String json = JsonUtil.toJson(complexObject);

// 反序列化
MyClass obj = JsonUtil.fromJson(json, MyClass.class);
List<MyClass> list = JsonUtil.fromJsonArray(jsonArray, MyClass.class);

// JSON 驗證
boolean valid = JsonUtil.isValidJson(jsonString);

// 格式化輸出
String prettyJson = JsonUtil.toPrettyJson(object);
```

#### 分散式唯一 ID 生成

```java
import com.vscodelife.socketio.util.SnowflakeUtil;

// 創建 ID 生成器（machineId 需在集群中唯一）
SnowflakeUtil.SnowflakeGenerator generator = SnowflakeUtil.createGenerator(1);

// 生成全局唯一 ID
long uniqueId = generator.nextId();

// 獲取 ID 詳細信息
SnowflakeUtil.IdInfo idInfo = SnowflakeUtil.parseId(uniqueId);
System.out.println("時間戳: " + idInfo.getTimestamp());
System.out.println("機器ID: " + idInfo.getMachineId());
System.out.println("序列號: " + idInfo.getSequence());
```

#### 性能分析工具

```java
import com.vscodelife.socketio.util.profiler.ProfilerUtil;
import com.vscodelife.socketio.util.profiler.ProfilerCounter;

// 開始性能分析
ProfilerUtil.startProfiling("message-processing");

// 執行業務邏輯
processComplexOperation();

// 結束並獲取結果
ProfilerCounter counter = ProfilerUtil.stopProfiling("message-processing");
System.out.println("執行次數: " + counter.getCount());
System.out.println("總時間: " + counter.getTotalTime() + "ms");
System.out.println("平均時間: " + counter.getAverageTime() + "ms");
System.out.println("最大時間: " + counter.getMaxTime() + "ms");
System.out.println("最小時間: " + counter.getMinTime() + "ms");
```

#### 日期時間處理

```java
import com.vscodelife.socketio.util.DateUtil;

// 格式化日期
String formatted = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");

// 解析日期
Date parsed = DateUtil.parse("2025-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss");

// 日期計算
Date tomorrow = DateUtil.addDays(new Date(), 1);
Date nextWeek = DateUtil.addWeeks(new Date(), 1);
Date nextMonth = DateUtil.addMonths(new Date(), 1);

// 日期比較
boolean isAfter = DateUtil.isAfter(date1, date2);
boolean isBefore = DateUtil.isBefore(date1, date2);
long diffDays = DateUtil.daysBetween(date1, date2);
```

#### HTTP 客戶端工具

```java
import com.vscodelife.socketio.util.http.HttpUtil;
import com.vscodelife.socketio.util.http.HttpResponse;

// GET 請求
HttpResponse response = HttpUtil.get("https://api.example.com/users");
if (response.isSuccess()) {
    String jsonData = response.getBody();
}

// POST 請求
Map<String, String> headers = new HashMap<>();
headers.put("Content-Type", "application/json");

HttpResponse postResponse = HttpUtil.post(
    "https://api.example.com/users", 
    JsonUtil.toJson(userData),
    headers
);

// 設置超時
HttpResponse timeoutResponse = HttpUtil.get("https://slow-api.com", 5000);
```

#### JWT 處理工具

```java
import com.vscodelife.socketio.util.JwtUtil;

// 生成 JWT
String secretKey = "your-secret-key";
Map<String, Object> claims = new HashMap<>();
claims.put("userId", 123);
claims.put("username", "john");

String token = JwtUtil.generateToken(claims, secretKey, 3600); // 1小時過期

// 驗證和解析 JWT
if (JwtUtil.validateToken(token, secretKey)) {
    Map<String, Object> parsedClaims = JwtUtil.parseToken(token, secretKey);
    Integer userId = (Integer) parsedClaims.get("userId");
    String username = (String) parsedClaims.get("username");
}

// 檢查是否過期
boolean isExpired = JwtUtil.isTokenExpired(token, secretKey);
```

### 5. 連接管理接口

```java
import com.vscodelife.socketio.connection.IConnection;

// 實現自定義連接
public class MyConnection implements IConnection<ByteArrayBuffer> {
    private long sessionId;
    private String userId;
    private Date connectTime;
    private AtomicLong lastActiveTime = new AtomicLong();
    
    @Override
    public long getSessionId() {
        return sessionId;
    }
    
    @Override
    public void updateLastActiveTime() {
        lastActiveTime.set(System.currentTimeMillis());
    }
    
    @Override
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - lastActiveTime.get() > timeoutMs;
    }
    
    // 自定義業務方法
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
}
```

## 🛠️ 技術棧

| 組件 | 版本 | 用途 |
|------|------|------|
| **Java** | 21 | 核心語言，支援最新特性 |
| **Netty** | 4.1.115.Final | 高性能網絡通信引擎 |
| **FastJSON** | 2.0.52 | 高性能 JSON 處理 |
| **Joda-Time** | 2.12.7 | 強大的日期時間 API |
| **JJWT** | 0.12.6 | JWT 令牌處理 |
| **Lombok** | 1.18.30 | 代碼簡化和增強 |

## 🔮 未來計劃

### 短期計劃 (v0.1.0)
- [ ] **增強註解系統**: 支援更多序列化選項
- [ ] **性能優化**: 進一步優化緩衝區操作性能
- [ ] **測試覆蓋**: 達到 95% 以上的測試覆蓋率
- [ ] **文檔完善**: 完整的 API 文檔和最佳實踐

## 📚 相關模組

在 TinySocket 生態系統中，socketio 作為核心庫被以下模組使用：

### 🏗️ 已實現模組
- **[serversocket](../serversocket/)**: 服務器端 Socket 實現
  - 基於 SocketBase 泛型設計
  - ByteSocket/JsonSocket 高性能服務器
  - 組件化架構和 Spring Boot 整合
- **[clientsocket](../clientsocket/)**: 客戶端 Socket 實現
  - 智能重連和心跳保持機制
  - 泛型客戶端架構設計
  - 協議處理器註冊系統
- **[demo](../demo/)**: 完整示範應用
  - 服務器和客戶端完整實現示例
  - **聊天系統**: 基於JsonSocket的完整聊天應用
  - **Web界面**: 現代化的聊天室前端界面
  - **管理組件**: ChatManager和UserManager實際業務組件
  - 協議處理和錯誤處理演示
  - 性能測試和最佳實踐展示

---

**由 vscodelife 團隊精心打造** ❤️  
*讓高性能網絡通信變得簡單而高效*

> **版本**: v0.0.1-SNAPSHOT  
> **最後更新**: 2025年9月13日  
> **Java版本**: OpenJDK 21+  
> **技術棧**: Netty 4.1.115 + FastJSON 2.0.52 + Joda-Time 2.12.7
> **新增功能**: 協議掃描增強 + 聊天系統支援

[![GitHub Stars](https://img.shields.io/github/stars/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Forks](https://img.shields.io/github/forks/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Issues](https://img.shields.io/github/issues/vscodelife/tinysocket)](https://github.com/vscodelife/tinysocket/issues)
[![License](https://img.shields.io/github/license/vscodelife/tinysocket)](../LICENSE)
