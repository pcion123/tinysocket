# SocketIO Core Library

這是tinysocket專案的核心Socket通信庫，提供了完整的網絡通信基礎設施和工具類。

## 📋 項目概述

SocketIO模塊是tinysocket框架的核心組件，提供了一套完整的Socket通信解決方案。它不是傳統的Socket.IO實現，而是一個基於Netty的自定義Socket通信框架，為整個tinysocket生態系統提供基礎服務。

## 🏗️ 架構設計

### 核心組件

```
socketio/
├── annotation/          # 註解定義
│   └── MessageTag.java  # 訊息欄位標記註解
├── buffer/              # 緩衝區管理
│   └── ByteArrayBuffer.java  # 可重用位元組緩衝區
├── connection/          # 連接管理
│   └── IConnection.java # 連接接口定義
├── message/             # 訊息處理
│   ├── base/           # 基礎訊息類
│   ├── ByteMessage.java # 位元組訊息
│   └── JsonMessage.java # JSON訊息
└── util/               # 工具類集合
    ├── profiler/       # 性能分析工具
    ├── http/          # HTTP工具
    └── 各種工具類
```

## 🚀 核心功能

### 1. 高性能緩衝區管理 (ByteArrayBuffer)

提供類似Netty ByteBuf的API，支援：

- **雙字節序支援**: Big-Endian（網絡字節序）和 Little-Endian
- **自動擴容**: 智能容量管理，避免頻繁記憶體分配
- **類型安全**: 支援所有Java基本類型和複雜對象
- **結構化序列化**: 基於@MessageTag註解的自動序列化

```java
// 創建緩衝區
ByteArrayBuffer buffer = new ByteArrayBuffer();

// 寫入基本類型
buffer.writeInt(42)
      .writeLong(System.currentTimeMillis())
      .writeString("Hello World")
      .writeBool(true);

// 讀取數據
int value = buffer.readInt();
long timestamp = buffer.readLong();
String message = buffer.readString();
boolean flag = buffer.readBool();
```

### 2. 結構化訊息系統

支援基於註解的自動訊息序列化：

```java
public class UserMessage {
    @MessageTag(order = 1)
    private int userId;
    
    @MessageTag(order = 2)
    private String username;
    
    @MessageTag(order = 3)
    private Date loginTime;
}

// 序列化
buffer.writeStruct(userMessage);

// 反序列化
UserMessage message = buffer.readStruct(UserMessage.class);
```

### 3. 多格式訊息支援

- **ByteMessage**: 高性能二進制訊息
- **JsonMessage**: 人類可讀的JSON格式訊息
- **自定義格式**: 可擴展的訊息格式

### 4. 豐富的工具類庫

#### 核心工具類
- **JsonUtil**: FastJSON 2.x封裝，支援完整的JSON處理
- **DateUtil**: Joda-Time封裝，提供強大的日期時間處理
- **SnowflakeUtil**: 分散式唯一ID生成器
- **Base64Util**: Base64編解碼工具
- **StrUtil**: 字串處理增強工具
- **ExecutorUtil**: 線程池管理工具

#### HTTP工具類
- **HttpUtil**: HTTP客戶端封裝
- **HttpResponse**: HTTP響應包裝類

#### 性能分析工具
- **ProfilerUtil**: 性能分析主工具
- **ProfilerCounter**: 性能計數器
- **ProfilerCounterManager**: 計數器管理
- **ProfilerConfig**: 分析器配置

## 🛠️ 技術棧

- **Java 21**: 現代Java特性支援
- **Spring Boot 3.5.4**: 應用框架基礎
- **Netty 4.1.115**: 高性能網絡通信
- **FastJSON 2.0.52**: 高性能JSON處理
- **Joda-Time 2.12.7**: 日期時間處理
- **Lombok 1.18.30**: 代碼簡化

## 📦 Maven配置

```xml
<dependency>
    <groupId>com.vscodelife</groupId>
    <artifactId>socketio</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 💡 使用示例

### 基本緩衝區操作

```java
// 創建可重用緩衝區
ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

// 寫入複雜數據
buffer.writeString("User Data")
      .writeInt(userId)
      .writeLong(timestamp)
      .writeJson(userObject)
      .writeBigDecimal(amount);

// 轉換為字節數組進行網絡傳輸
byte[] data = buffer.toByteArray();

// 從字節數組讀取
ByteArrayBuffer readBuffer = new ByteArrayBuffer();
readBuffer.writeBytes(data);
readBuffer.readerIndex(0);

String title = readBuffer.readString();
int id = readBuffer.readInt();
// ...
```

### JSON處理

```java
// 對象序列化
Map<String, Object> data = Map.of(
    "id", 123,
    "name", "John",
    "active", true
);
String json = JsonUtil.toJson(data);

// JSON反序列化
User user = JsonUtil.fromJson(json, User.class);

// JSON驗證
boolean isValid = JsonUtil.isValidJson(jsonString);
```

### 性能分析

```java
// 啟用性能分析
ProfilerUtil.startProfiling("message-processing");

// 業務邏輯
processMessage(message);

// 結束分析並獲取結果
ProfilerCounter counter = ProfilerUtil.stopProfiling("message-processing");
System.out.println("Processing time: " + counter.getAverageTime() + "ms");
```

### 唯一ID生成

```java
// 初始化雪花算法（machineId需要在集群中唯一）
SnowflakeUtil.IdInfo idInfo = SnowflakeUtil.generateId(1);
long uniqueId = idInfo.getId();
System.out.println("Generated ID: " + uniqueId);
```

## 🔧 高級特性

### 1. 字節序支援

```java
// 大端序（網絡字節序，默認）
ByteArrayBuffer bigEndianBuffer = new ByteArrayBuffer(1024, ByteOrder.BIG_ENDIAN);

// 小端序（Intel x86架構）
ByteArrayBuffer littleEndianBuffer = new ByteArrayBuffer(1024, ByteOrder.LITTLE_ENDIAN);
```

### 2. 內存優化

- **零拷貝設計**: 最小化內存分配和複製
- **智能擴容**: 避免頻繁的數組重分配
- **重用機制**: 支援緩衝區重複使用

### 3. 類型安全

- **泛型支援**: 完整的泛型類型系統
- **自動類型轉換**: 智能的類型推斷和轉換
- **運行時檢查**: 提供詳細的錯誤信息

## 🧪 測試

```bash
# 運行所有測試
mvn test

# 運行特定測試類
mvn test -Dtest=JsonUtilTest

# 生成測試報告
mvn test jacoco:report
```

## 📈 性能特性

- **高吞吐量**: 優化的緩衝區管理，支援高並發場景
- **低延遲**: 最小化對象創建和垃圾回收
- **內存效率**: 智能的內存管理和重用機制
- **可擴展性**: 支援大規模分散式部署

## 🔮 未來計劃

- [ ] 支援更多序列化格式（Protobuf、MessagePack）
- [ ] 添加加密和壓縮支援
- [ ] 實現連接池和負載均衡
- [ ] 集成Spring Boot自動配置
- [ ] 添加監控和度量指標
- [ ] 支援響應式程式設計模式

## 📚 相關模組

在tinysocket生態系統中，socketio作為核心庫被以下模組使用：

- **serversocket**: 服務器端Socket實現
- **clientsocket**: 客戶端Socket實現  
- **websocket**: WebSocket協議支援
- **webserver**: HTTP服務器實現

## 🤝 貢獻

歡迎提交Issue和Pull Request來改進這個項目。

## 📄 許可證

本項目採用MIT許可證 - 查看 [LICENSE](../LICENSE) 文件了解詳情。

---

**由 vscodelife 團隊開發和維護**
