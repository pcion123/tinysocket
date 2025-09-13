# TinySocket 高性能網絡通信框架

<p align="center">
  <img src="https://img.shields.io/badge/Java-21+-blue.svg" alt="Java Version">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.4-green.svg" alt="Spring Boot Version">
  <img src="https://img.shields.io/badge/Netty-4.1.115-orange.svg" alt="Netty Version">
  <img src="https://img.shields.io/badge/License-MIT-brightgreen.svg" alt="License">
</p>

TinySocket 是一款基於 **Java 21** 和 **Netty 4.1.115** 構建的現代化、高性能網絡通信框架。它提供了完整的客戶端-服務器通信解決方案，支援**二進制**和 **JSON** 雙重通信協議，採用**泛型設計**和**註解驅動**開發模式，為企業級應用提供生產就緒的Socket通信基礎設施。

## 🌟 核心特性

### 🚀 高性能設計
- **零拷貝緩衝區**: 基於 Netty 的高效內存管理，減少 GC 壓力
- **異步 I/O**: 支援高並發連接，單機可處理數萬連接
- **智能快取**: 內建訊息快取和連接池管理
- **性能監控**: 內建 ProfilerUtil 實時性能分析

### 🔧 現代化架構
- **泛型設計**: 完整的泛型約束確保類型安全
- **註解驅動**: 使用 `@ProtocolTag` 和 `@MessageTag` 簡化開發
- **組件化**: 可插拔的限流器、協議處理器等組件
- **Spring Boot 整合**: 無縫整合 Spring Boot 生態系統

### 📨 雙協議支援
- **ByteSocket**: 高效二進制通信，適用於遊戲、IoT等高性能場景
- **JsonSocket**: JSON 通信協議，適用於 Web API、微服務等場景
- **WebSocket 支援**: 原生支援 WebSocket 協議
- **協議混合**: 同時支援兩種協議的混合使用

### 🛠️ 開發友好
- **豐富工具類**: JSON處理、JWT認證、HTTP客戶端、日期工具等
- **完整示例**: 包含聊天系統、遊戲服務器等完整示例
- **詳細文檔**: 每個模組都有詳細的API文檔和使用指南
- **測試覆蓋**: 完整的單元測試和整合測試

## 🏗️ 模組架構

TinySocket 框架採用模組化設計，由四個核心模組組成：

```
TinySocket Framework
├── socketio/           # 🔧 核心通信庫
│   ├── 雙緩衝區系統        # ByteArrayBuffer + JsonMapBuffer
│   ├── 註解驅動開發        # @ProtocolTag + @MessageTag
│   ├── 豐富工具類庫        # JSON/JWT/HTTP/Date 工具
│   └── 協議處理系統        # 協議註冊、快取管理、異常處理
├── serversocket/       # 🚀 服務器端實現
│   ├── SocketBase 泛型基類  # 完整的泛型約束設計
│   ├── ByteSocket 服務器    # 高性能二進制通信
│   ├── JsonSocket 服務器    # JSON + WebSocket 支援
│   └── 組件化系統          # 限流器、協議捕獲器、連接管理
├── clientsocket/       # 🔗 客戶端實現
│   ├── 自動重連機制        # 智能重連策略
│   ├── 心跳保持           # 連接活性檢測
│   ├── 訊息快取           # 離線訊息重發
│   └── 多協議支援          # ByteSocket + JsonSocket
└── demo/              # 📚 完整示例集合
    ├── 二進制通信演示       # 遊戲風格的高性能通信
    ├── Web聊天系統        # 現代化聊天室實現
    ├── Spring Boot整合    # 完整的企業級配置
    └── 打包部署方案        # 生產環境部署指南
```

---

## 📚 模組詳細說明

### 🔧 [SocketIO](socketio/) - 核心通信庫
**作為整個框架的基石，提供統一的通信協議和基礎設施**

| 功能類別 | 核心組件 | 功能描述 |
|---------|---------|---------|
| **緩衝區管理** | `ByteArrayBuffer` | 零拷貝二進制緩衝區，支援壓縮和雙字節序 |
| | `JsonMapBuffer` | 高性能JSON緩衝區，基於FastJSON2實現 |
| **訊息系統** | `ByteMessage` / `JsonMessage` | 結構化訊息處理，支援泛型設計 |
| | `ByteCache` / `JsonCache` | 智能訊息快取管理 |
| **註解驅動** | `@ProtocolTag` | 協議方法自動註冊 |
| | `@MessageTag` | 序列化欄位標記 |
| **工具類庫** | `JsonUtil` / `JwtUtil` / `HttpUtil` | 豐富的工具類支援 |
| | `SnowflakeUtil` / `ProfilerUtil` | 分散式ID和性能分析 |

**🎯 適用場景**: 作為其他模組的基礎依賴，通常不直接使用

### 🚀 [ServerSocket](serversocket/) - 服務器端實現
**高性能、高併發的Socket服務器框架，支援泛型設計和組件化架構**

| 服務器類型 | 適用場景 | 核心特性 |
|-----------|---------|---------|
| **ByteSocket** | 遊戲服務器、IoT設備、高頻交易 | 二進制協議、極致性能、低延遲 |
| **JsonSocket** | Web API、微服務、聊天系統 | JSON協議、WebSocket支援、易於調試 |
| **SocketBase** | 自定義服務器 | 泛型基類、完整約束、靈活擴展 |

**核心組件**:
- **RateLimiter**: 多級限流保護，支援隨機過濾和時間窗口
- **ProtocolRegister**: 註解驅動的協議自動註冊
- **ProtocolCatcher**: 協議異常捕獲和優雅降級
- **Connection管理**: 智能連接生命周期管理

**🎯 適用場景**: 需要構建高性能Socket服務器的企業級應用

### 🔗 [ClientSocket](clientsocket/) - 客戶端實現
**智能化的Socket客戶端，內建重連、心跳、快取等企業級特性**

| 客戶端類型 | 連接特性 | 可靠性保障 |
|-----------|---------|-----------|
| **ByteClient** | 二進制協議、高性能 | 自動重連、訊息快取、心跳保持 |
| **JsonClient** | JSON協議、易於整合 | 連接監控、異常恢復、狀態同步 |

**智能特性**:
- **自動重連**: 指數退避算法，智能重連策略
- **心跳保持**: 定期心跳檢測，確保連接活性
- **訊息快取**: 離線訊息暫存，連接恢復後自動重發
- **狀態管理**: 完整的連接狀態機管理

**🎯 適用場景**: 需要可靠Socket連接的客戶端應用

### 📚 [Demo](demo/) - 完整示例集合
**提供完整的使用示例和最佳實踐，是學習TinySocket的最佳起點**

| 示例類型 | 技術棧 | 學習重點 |
|---------|-------|---------|
| **二進制通信系統** | ByteSocket + Console | 高性能協議設計、認證機制 |
| **Web聊天系統** | JsonSocket + WebSocket + HTML | 實時通信、用戶管理、前端整合 |
| **Spring Boot整合** | 完整企業級配置 | 生產環境部署、配置管理 |

**示例功能**:
- 🔐 **用戶認證**: JWT Token管理、登入登出
- 💬 **實時通信**: 即時訊息、廣播、私聊
- 📊 **監控統計**: 連接數監控、性能統計
- 🌐 **Web界面**: 現代化聊天室前端

**🎯 適用場景**: 框架學習、原型開發、生產環境參考

---

## ⚡ 快速開始

### 環境要求

| 組件 | 版本要求 | 說明 |
|------|---------|------|
| **Java** | 21+ | 使用最新的Java特性和性能優化 |
| **Maven** | 3.6+ | 構建和依賴管理 |
| **操作系統** | Windows/Linux/macOS | 跨平台支援 |

### 方式一：運行示例程序（推薦）

Clone專案並運行完整示例：

```bash
# 克隆專案
git clone https://github.com/vscodelife/tinysocket.git
cd tinysocket

# 編譯整個專案
mvn clean package

# 方式1: 運行二進制通信演示
cd demo/target
# 解壓並運行
unzip demo-0.0.1-SNAPSHOT.zip
cd demo-0.0.1-SNAPSHOT

# 啟動服務器（PowerShell/CMD）
.\run-byte-server.bat

# 新開命令行，啟動客戶端
.\run-byte-client.bat user1 password123

# 方式2: 運行Web聊天系統
.\run-web-chat.bat
# 然後在瀏覽器訪問: http://localhost:30002
```

### 方式二：整合到現有專案

#### 1. 添加Maven依賴

```xml
<dependencies>
    <!-- 服務器端開發 -->
    <dependency>
        <groupId>com.vscodelife</groupId>
        <artifactId>serversocket</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
    
    <!-- 客戶端開發 -->
    <dependency>
        <groupId>com.vscodelife</groupId>
        <artifactId>clientsocket</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>
</dependencies>
```

#### 2. 創建簡單的服務器

```java
// 創建JSON服務器
public class MyApiServer extends JsonSocket<ApiHeader, ApiConnection> {
    
    public MyApiServer() {
        super(logger, 8080, 1000, MyInitializer.class);
        // 註冊協議處理器
        protocolRegister.scanAndRegisterProtocols(MyProtocol.class);
    }
    
    @Override
    protected Class<ApiConnection> getConnectionClass() {
        return ApiConnection.class;
    }
    
    @Override
    public String getVersion() { return "1.0.0"; }
}

// 協議處理器
public class MyProtocol {
    public static MyApiServer server;
    
    @ProtocolTag(mainNo = 1, subNo = 1, describe = "用戶登入")
    public static void handleLogin(JsonMessage<ApiHeader> message) {
        JsonMapBuffer buffer = message.getBuffer();
        String username = buffer.getString("username");
        String password = buffer.getString("password");
        
        // 處理登入邏輯...
        JsonMapBuffer response = new JsonMapBuffer();
        response.put("success", true);
        response.put("token", "jwt-token-here");
        
        server.sendToClient(message.getHeader().getSessionId(), 1, 1, response);
    }
}

// 啟動服務器
public static void main(String[] args) {
    MyApiServer server = new MyApiServer();
    server.run();
}
```

#### 3. 創建客戶端連接

```java
// 創建JSON客戶端
JsonClient<ApiHeader> client = new JsonClient<>(
    "localhost", 8080, 
    MyClientInitializer.class, 
    ApiHeader.class
);

// 註冊協議處理器
client.getProtocolRegister().scanAndRegisterProtocols(ClientProtocol.class);

// 連接服務器
client.connect();

// 發送登入請求
JsonMapBuffer loginData = new JsonMapBuffer();
loginData.put("username", "alice");
loginData.put("password", "password123");
client.sendMessage(1, 1, loginData);
```

---

## 🛠️ 技術棧

TinySocket 框架基於現代化的Java技術棧構建：

| 類別 | 組件 | 版本 | 用途 |
|------|------|------|------|
| **核心框架** | Java | 21+ | 基礎語言，支援虛擬線程和模式匹配 |
| | Netty | 4.1.115.Final | 高性能網絡通信引擎 |
| | Spring Boot | 3.5.4 | 應用框架和依賴注入 |
| **序列化** | FastJSON | 2.0.52 | 高性能JSON序列化 |
| **工具庫** | JJWT | 0.12.6 | JWT令牌處理 |
| | Joda-Time | 2.12.7 | 日期時間處理 |
| | Lombok | 1.18.30 | 代碼簡化 |
| **構建工具** | Maven | 3.6+ | 依賴管理和構建 |

### 性能特性

- **零拷貝**: ByteArrayBuffer實現零拷貝操作，減少內存分配
- **異步I/O**: 基於Netty NIO，支援數萬並發連接
- **智能快取**: 訊息對象池化，降低GC壓力
- **壓縮支援**: 內建GZIP壓縮，節省網絡帶寬

---

## 🎯 使用場景

TinySocket 框架適用於多種應用場景：

### 🎮 遊戲服務器
- **實時遊戲**: MMORPG、MOBA、FPS等需要低延遲的遊戲
- **回合制遊戲**: 棋牌、策略類遊戲的服務器實現
- **遊戲大廳**: 房間匹配、玩家管理、遊戲監控

### 💬 即時通信
- **企業IM**: 企業內部即時通訊系統
- **在線客服**: 客戶服務和技術支援系統
- **社交聊天**: 群聊、私聊、語音文字混合通信

### 🌐 Web服務
- **WebSocket API**: RESTful API的補充，提供雙向通信
- **推送服務**: 消息推送、通知系統
- **實時數據**: 股票行情、監控數據的實時推送

### 🏭 物聯網 (IoT)
- **設備通信**: 智能家居、工業設備的數據採集
- **邊緣計算**: 邊緣節點與雲端的高效通信
- **監控系統**: 實時監控數據傳輸和處理

## 📖 學習路徑

### 🎯 初學者路徑
1. **[Demo示例](demo/README.md)** - 運行完整示例，理解框架整體功能
2. **[SocketIO文檔](socketio/README.md)** - 學習核心概念和基礎API
3. **簡單實踐** - 基於示例修改，實現自己的小功能

### 🔧 開發者路徑
1. **[ServerSocket文檔](serversocket/README.md)** - 深入理解服務器端架構
2. **[ClientSocket文檔](clientsocket/README.md)** - 掌握客戶端開發技巧
3. **生產實踐** - 構建完整的商業級應用

### 🚀 架構師路徑
1. **源碼研究** - 深入理解框架設計模式和最佳實踐
2. **性能調優** - 針對特定場景進行性能優化
3. **擴展開發** - 基於框架開發自定義組件和擴展

---

**由 vscodelife 團隊精心打造** ❤️  
*讓網絡通信變得簡單而高效*

> **版本**: v0.0.1-SNAPSHOT  
> **最後更新**: 2025年9月14日  
> **Java版本**: OpenJDK 21+  
> **Spring Boot版本**: 3.5.4
> **模組狀態**: socketio ✅ | serversocket ✅ | clientsocket ✅ | demo ✅

[![GitHub Stars](https://img.shields.io/github/stars/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Forks](https://img.shields.io/github/forks/vscodelife/tinysocket?style=social)](https://github.com/vscodelife/tinysocket)
[![GitHub Issues](https://img.shields.io/github/issues/vscodelife/tinysocket)](https://github.com/vscodelife/tinysocket/issues)
[![License](https://img.shields.io/github/license/vscodelife/tinysocket)](./LICENSE)
