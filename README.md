# TinySocket 高性能網絡通信框架

TinySocket是一個基於Netty的高性能、模組化網絡通信框架，提供完整的Socket通信解決方案。

## 🏗️ 專案架構

TinySocket採用Maven多模組架構設計，目前包含核心基礎庫和配置管理：

```
tinysocket/
├── pom/                    # Maven父專案配置
│   └── pom.xml            # 父POM文件（依賴管理）
├── socketio/              # 核心Socket通信庫 ⭐
│   ├── src/main/java/     # 核心API和工具類
│   │   └── com/vscodelife/socketio/
│   │       ├── annotation/    # 註解系統
│   │       ├── buffer/        # 高性能緩衝區
│   │       ├── connection/    # 連接管理
│   │       ├── message/       # 訊息處理
│   │       └── util/          # 豐富工具類庫
│   └── pom.xml
├── .vscode/               # VS Code開發配置
├── mvnw & mvnw.cmd       # Maven Wrapper
└── README.md
```

### 🎯 設計理念

- **高性能**: 基於Netty NIO，支援高並發場景
- **模組化**: 清晰的模組邊界，易於擴展和維護
- **類型安全**: 完整的泛型支援和運行時檢查
- **開發友好**: 豐富的工具類和詳細的錯誤信息

## 🚀 核心特性

### 🔧 SocketIO 核心庫

作為整個框架的基石，socketio模組提供：

#### 💾 高性能緩衝區管理
- **ByteArrayBuffer**: 可重用、零拷貝的位元組緩衝區
- **雙字節序支援**: Big-Endian（網絡字節序）& Little-Endian
- **智能擴容**: 自動記憶體管理，避免頻繁分配

#### 📨 結構化訊息系統
- **@MessageTag註解**: 自動序列化/反序列化
- **多格式支援**: JSON、二進制、自定義格式
- **版本相容**: 向前/向後相容的協議設計

#### 🛠️ 豐富工具類庫
- **JsonUtil**: FastJSON 2.x高性能JSON處理
- **SnowflakeUtil**: 分散式唯一ID生成器
- **ProfilerUtil**: 內建性能分析工具
- **DateUtil**: Joda-Time日期時間處理
- **HttpUtil**: HTTP客戶端封裝

## 🛠️ 技術棧

| 組件 | 版本 | 用途 |
|------|------|------|
| **Java** | 21 | 核心語言，支援最新特性 |
| **Spring Boot** | 3.5.4 | 應用框架和自動配置 |
| **Netty** | 4.1.115.Final | 高性能網絡通信引擎 |
| **FastJSON** | 2.0.52 | 高性能JSON處理 |
| **Joda-Time** | 2.12.7 | 強大的日期時間API |
| **Lombok** | 1.18.30 | 代碼簡化和增強 |
| **Maven** | 3.9+ | 專案構建和依賴管理 |

## ⚡ 快速開始

### 📋 環境需求

- **JDK**: OpenJDK 21或更高版本
- **Maven**: Apache Maven 3.9+  
- **IDE**: VS Code（推薦）或IntelliJ IDEA

### 🚀 安裝和構建

```bash
# 1. 克隆專案
git clone <repository-url>
cd tinysocket

# 2. 使用Maven Wrapper構建（推薦）
./mvnw clean compile  # Linux/macOS
mvnw.cmd clean compile  # Windows

# 3. 或使用本地Maven
mvn clean compile -f pom/pom.xml
```

### 💡 核心API使用示例

#### 高性能緩衝區操作
```java
import com.vscodelife.socketio.buffer.ByteArrayBuffer;

// 創建可重用緩衝區
ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

// 寫入不同類型的數據
buffer.writeString("TinySocket")
      .writeInt(2025)
      .writeLong(System.currentTimeMillis())
      .writeJson(userObject);

// 轉換為字節數組進行網絡傳輸
byte[] data = buffer.toByteArray();

// 讀取數據
buffer.clear().writeBytes(data);
String framework = buffer.readString();
int year = buffer.readInt();
long timestamp = buffer.readLong();
```

#### 結構化訊息序列化
```java
import com.vscodelife.socketio.annotation.MessageTag;

public class UserMessage {
    @MessageTag(order = 1)
    private int userId;
    
    @MessageTag(order = 2) 
    private String username;
    
    @MessageTag(order = 3)
    private Date loginTime;
}

// 自動序列化/反序列化
buffer.writeStruct(userMessage);
UserMessage received = buffer.readStruct(UserMessage.class);
```

#### JSON高性能處理
```java
import com.vscodelife.socketio.util.JsonUtil;

// 序列化（支援null值）
String json = JsonUtil.toJson(complexObject);

// 反序列化
MyClass obj = JsonUtil.fromJson(json, MyClass.class);

// JSON驗證
boolean valid = JsonUtil.isValidJson(jsonString);
```

#### 分散式ID生成
```java
import com.vscodelife.socketio.util.SnowflakeUtil;

// 生成全局唯一ID（machineId需在集群中唯一）
SnowflakeUtil.IdInfo idInfo = SnowflakeUtil.generateId(1);
long uniqueId = idInfo.getId();
```

#### 性能分析
```java
import com.vscodelife.socketio.util.profiler.ProfilerUtil;

// 開始性能分析
ProfilerUtil.startProfiling("message-processing");

// 執行業務邏輯
processComplexOperation();

// 結束並獲取結果
ProfilerCounter counter = ProfilerUtil.stopProfiling("message-processing");
System.out.println("平均處理時間: " + counter.getAverageTime() + "ms");
```

## 🔧 開發指南

### Maven依賴管理

專案使用父POM統一管理依賴版本：

```xml
<!-- 在你的專案中引用 -->
<dependency>
    <groupId>com.vscodelife</groupId>
    <artifactId>socketio</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### VS Code開發環境

專案已配置完整的VS Code開發支援：

- ✅ **自動格式化**: Google Java Style
- ✅ **智能提示**: 完整的代碼補全
- ✅ **調試支援**: 一鍵啟動調試
- ✅ **Maven整合**: 內建構建和測試

### 🧪 測試

```bash
# 運行所有測試
./mvnw test

# 運行特定模組測試
./mvnw test -pl socketio

# 生成測試報告
./mvnw clean test jacoco:report
```

## 📈 性能特性

| 特性 | 說明 | 優勢 |
|------|------|------|
| **零拷貝緩衝區** | 智能內存管理 | 減少GC壓力，提升吞吐量 |
| **字節序優化** | 網絡/本地字節序切換 | 跨平台高效數據交換 |
| **結構化序列化** | 註解驅動自動化 | 類型安全，性能優於反射 |
| **連接池化** | 可重用連接管理 | 降低連接建立開銷 |
| **異步處理** | 基於Netty NIO | 支援高並發場景 |

## 🔮 發展計劃

### 🎯 短期目標（下個版本）
- [ ] **serversocket**: 服務器端Socket實現
- [ ] **clientsocket**: 客戶端Socket實現
- [ ] **Spring Boot整合**: 自動配置和Starter
- [ ] **連接池**: 高效的連接管理

### 🚀 中期目標（未來2-3個版本）
- [ ] **websocket**: WebSocket協議支援
- [ ] **負載均衡**: 多節點負載分發
- [ ] **安全增強**: SSL/TLS和認證機制
- [ ] **監控儀表板**: 實時性能監控

### 🌟 長期願景
- [ ] **微服務整合**: Service Mesh支援
- [ ] **雲原生**: Kubernetes Operator
- [ ] **多語言支援**: 跨語言客戶端
- [ ] **AI增強**: 智能路由和預測

## 🏆 核心優勢

### 🎨 **開發體驗**
- 豐富的API文檔和示例
- 完整的IDE支援
- 詳細的錯誤信息和調試

### ⚡ **高性能**
- 基於Netty的異步I/O
- 零拷貝內存管理
- 智能的連接復用

### 🔧 **易擴展**
- 模組化架構設計
- 清晰的API邊界
- 插件化擴展機制

### 🛡️ **產品級**
- 完善的錯誤處理
- 內建性能監控
- 生產環境驗證

## 📚 學習資源

- 📖 **[API文檔](./socketio/README.md)**: 詳細的API使用指南
- 🎯 **[最佳實踐](#)**: 生產環境使用建議
- 🔧 **[擴展開發](#)**: 如何開發自定義擴展
- 📊 **[性能調優](#)**: 性能優化技巧

## 🤝 社群與支援

- 💬 **GitHub Issues**: 問題報告和功能請求
- 📧 **郵件支援**: vscodelife@example.com
- 📱 **技術交流**: 加入開發者討論群

## 📄 許可證

本專案採用 **MIT License** - 查看 [LICENSE](LICENSE) 文件了解詳情。

---

## 🙏 致謝

感謝以下開源項目的貢獻：
- [Netty](https://netty.io/) - 高性能網絡應用框架
- [Spring Boot](https://spring.io/projects/spring-boot) - 現代化Java應用框架
- [FastJSON](https://github.com/alibaba/fastjson2) - 高性能JSON庫

---

**由 vscodelife 團隊精心打造** ❤️

> *讓網絡通信變得簡單而高效*
