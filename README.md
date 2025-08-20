# TinySocket 多模組專案

這是一個基於Maven的多模組Spring Boot專案，用於開發各種Socket相關的功能。

## 專案結構

```
tinysocket/
├── pom.xml                 # 父專案POM文件
├── pom/                    # 原始tinysocket模組
│   ├── pom.xml            # 子模組POM文件
│   ├── src/               # 源代碼目錄
│   └── target/            # 編譯輸出目錄
└── (待創建的子模組)
    ├── socketio/           # Socket.IO模組
    ├── serversocket/       # 服務器Socket模組
    ├── clientsocket/       # 客戶端Socket模組
    ├── websocket/          # WebSocket模組
    └── webserver/          # Web服務器模組
```

## 配置說明

### 父專案 (tinysocket-parent)
- **GroupId**: `com.vscodelife`
- **ArtifactId**: `tinysocket-parent`
- **Packaging**: `pom`
- **Java版本**: 21
- **Spring Boot版本**: 3.5.4

### 已配置的子模組

1. **pom** - 原始的tinysocket專案，包含:
   - Spring Boot Starter Web
   - Spring Boot Starter Data JPA
   - H2 Database
   - Spring Boot DevTools
   - 環境配置檔案 (dev/prod)
   - 自動版本生成功能

### 待創建的子模組

在父專案的pom.xml中已預配置了以下模組（目前被註解掉）：

```xml
<modules>
    <module>pom</module>
    <!-- 以下模組將在創建後啟用 -->
    <!-- <module>../socketio</module> -->
    <!-- <module>../serversocket</module> -->
    <!-- <module>../clientsocket</module> -->
    <!-- <module>../websocket</module> -->
    <!-- <module>../webserver</module> -->
</modules>
```

## 使用方式

### 構建整個專案
```bash
mvn clean install
```

### 構建特定模組
```bash
cd pom
mvn clean install
```

### 運行開發環境
```bash
cd pom
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 運行生產環境
```bash
cd pom
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 下一步

1. 在工作區根目錄創建各個子模組目錄
2. 為每個子模組創建對應的pom.xml文件
3. 在父專案pom.xml中取消註解相關模組
4. 開始開發各個模組的功能

## 環境配置

專案支援多環境配置：
- **dev** (開發環境) - 預設啟用，包含詳細日誌和devtools
- **prod** (生產環境) - 最佳化配置，移除devtools

環境特定的配置文件應放在：
- `src/main/resources/dev/` - 開發環境配置
- `src/main/resources/prod/` - 生產環境配置

---

## 原始專案說明

TinySocket 是一個使用 Spring Boot 3.5.4 和 Java 21 構建的現代化 Java 應用程式，由 vscodelife 專案創建。

## 🚀 技術棧

- **Framework**: Spring Boot 3.5.4
- **Language**: Java 21 (OpenJDK)
- **Build Tool**: Apache Maven 3.9+
- **Database**: H2 Database (開發/測試用)
- **Web**: Spring Web MVC
- **Data Access**: Spring Data JPA
- **Development**: Spring Boot DevTools
- **Testing**: JUnit 5, Mockito, Spring Boot Test

## 📁 專案結構

```
tinysocket/
├── .vscode/                    # VS Code 開發配置
│   ├── settings.json          # 編輯器設定
│   └── launch.json            # 調試配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/vscodelife/tinysocket/
│   │   │       ├── TinysocketApplication.java    # 主應用程式類
│   │   │       └── Version.java                  # 自動生成的版本資訊
│   │   ├── resources/
│   │   │   ├── application.properties            # 預設配置
│   │   │   ├── dev/
│   │   │   │   └── application.properties        # 開發環境配置
│   │   │   └── prod/
│   │   │       └── application.properties        # 生產環境配置
│   │   └── assembly/
│   │       └── assembly.xml                      # 分發打包配置
│   └── test/
│       └── java/
│           └── com/vscodelife/tinysocket/
│               └── TinysocketApplicationTests.java  # 測試類
├── target/                    # Maven 建構輸出
├── .gitignore                # Git 忽略配置
├── .gitattributes           # Git 屬性配置
├── pom.xml                  # Maven 專案配置
├── run.bat                  # Windows 啟動腳本
└── README.md               # 專案說明文件
```

## 🛠️ 環境需求

- **JDK**: OpenJDK 21 或更高版本
- **Maven**: Apache Maven 3.9 或更高版本
- **IDE**: VS Code (推薦) 或任何支援 Java 的 IDE

## ⚡ 快速開始

### 1. 克隆專案
```bash
git clone <repository-url>
cd tinysocket
```

### 2. 編譯專案
```bash
# 開發環境編譯
mvn clean compile

# 生產環境編譯
mvn clean compile -Pprod
```

### 3. 運行應用程式

#### 開發模式 (推薦)
```bash
# 使用 Maven 啟動 (熱重載)
mvn spring-boot:run

# 或者使用批次檔
run.bat
```

#### 生產模式
```bash
# 先建構專案
mvn clean package -Pprod

# 運行 JAR 檔案
java -jar target/tinysocket-0.0.1-SNAPSHOT.jar

# 或者使用批次檔
run.bat prod
```

### 4. 存取應用程式
- **應用程式**: http://localhost:8080/tinysocket
- **H2 資料庫控制台** (僅開發環境): http://localhost:8080/tinysocket/h2-console
  - JDBC URL: `jdbc:h2:mem:tinysocket_dev`
  - 使用者名稱: `sa`
  - 密碼: (空白)

## 🔧 配置說明

### 環境設定

專案支援多環境配置：

- **開發環境 (`dev`)**: 預設啟用，包含詳細日誌、H2 控制台、熱重載
- **生產環境 (`prod`)**: 最佳化設定，停用開發工具，最小日誌

### Maven Profiles

```bash
# 開發環境 (預設)
mvn spring-boot:run -Pdev

# 生產環境
mvn spring-boot:run -Pprod
```

### 重要配置文件

- `src/main/resources/application.properties`: 基礎配置
- `src/main/resources/dev/application.properties`: 開發環境專用配置
- `src/main/resources/prod/application.properties`: 生產環境專用配置

## 🔨 建構與打包

### 完整建構
```bash
# 開發環境建構 (包含測試)
mvn clean package

# 生產環境建構 (跳過測試)
mvn clean package -Pprod
```

### 建構產出物
- `target/tinysocket-0.0.1-SNAPSHOT.jar`: 主要應用程式 JAR
- `target/tinysocket-0.0.1-SNAPSHOT-sources.jar`: 原始碼 JAR
- `target/tinysocket-{env}.zip`: 完整分發套件 (包含所有依賴)
- `target/lib/`: 所有依賴 JAR 檔案

## 🧪 測試

```bash
# 執行所有測試
mvn test

# 執行測試並生成報告
mvn clean test

# 跳過測試 (生產環境)
mvn clean package -Pprod
```

## 🚀 開發指導

### VS Code 開發環境

專案已配置完整的 VS Code 開發環境：

- **自動格式化**: 儲存時自動格式化程式碼
- **Google Java Style**: 使用 Google Java 程式碼風格
- **除錯配置**: 支援開發和除錯模式啟動
- **Copilot 整合**: 智慧型程式碼提示和提交訊息生成

### 自動生成功能

1. **版本資訊類別**: 每次建構時自動生成 `Version.java`
2. **環境特定資源**: 根據 Profile 自動載入對應環境配置
3. **依賴管理**: 自動複製所有依賴到 `target/lib/`

### 下一步開發建議

1. **建立 REST API**:
   ```java
   @RestController
   @RequestMapping("/api")
   public class TinySocketController {
       @GetMapping("/version")
       public Map<String, String> getVersion() {
           Map<String, String> version = new HashMap<>();
           version.put("version", Version.VERSION);
           version.put("buildTime", Version.BUILDTIME);
           version.put("environment", Version.ENVIRONMENT);
           return version;
       }
   }
   ```

2. **新增實體類別和 Repository**:
   ```java
   @Entity
   public class SocketMessage {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       private String content;
       // getters and setters
   }
   
   @Repository
   public interface SocketMessageRepository extends JpaRepository<SocketMessage, Long> {
   }
   ```

3. **整合 WebSocket**: 新增即時通訊功能
4. **API 文件**: 整合 OpenAPI/Swagger
5. **安全性**: 新增 Spring Security
6. **資料庫**: 切換到 PostgreSQL 或 MySQL

## 📋 專案檢查清單

### ✅ 已完成功能

- [x] 基礎 Spring Boot 專案架構
- [x] Maven 多環境支援 (dev/prod)
- [x] VS Code 開發環境配置
- [x] Git 版本控制
- [x] 自動版本資訊生成
- [x] 完整的建構和打包流程
- [x] 單元測試框架
- [x] H2 資料庫整合
- [x] Spring Data JPA 設定
- [x] 開發熱重載支援

### 🔄 待開發功能

- [ ] REST API 端點
- [ ] 資料模型設計
- [ ] 業務邏輯實作
- [ ] 前端介面
- [ ] API 文件
- [ ] 安全性配置
- [ ] 生產資料庫整合
- [ ] 監控和日誌
- [ ] Docker 容器化
- [ ] CI/CD 流水線

## 📝 版本資訊

- **當前版本**: 0.0.1-SNAPSHOT
- **最後建構**: 自動生成於建構時
- **環境**: 根據建構 Profile 決定

---

由 **vscodelife** 專案創建和維護。
