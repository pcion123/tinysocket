# Socket.IO Module

這是tinysocket專案的Socket.IO子模組，提供即時通訊功能。

## 功能特點

- **即時通訊**: 基於WebSocket的雙向即時通訊
- **Socket.IO協議**: 完整支援Socket.IO協議
- **自動重連**: 客戶端自動重連機制
- **廣播消息**: 支援向所有客戶端廣播消息
- **點對點消息**: 支援向特定客戶端發送消息
- **REST API**: 提供HTTP API用於消息發送

## 技術棧

- **Spring Boot 3.5.4**: 應用框架
- **Netty Socket.IO 2.0.11**: Socket.IO服務器實現
- **FastJSON 2.0.52**: JSON處理
- **Java 21**: 編程語言

## 配置

### 服務器配置
- **Socket.IO端口**: 9092
- **HTTP端口**: 8081
- **主機**: localhost

### 自定義配置
可在`application.properties`中修改：
```properties
socketio.host=localhost
socketio.port=9092
server.port=8081
```

## API端點

### 1. 廣播消息
```http
POST /api/socketio/broadcast
Content-Type: application/json

{
  "message": "Hello World!"
}
```

### 2. 發送給特定客戶端
```http
POST /api/socketio/send/{clientId}
Content-Type: application/json

{
  "message": "Hello Client!"
}
```

### 3. 服務狀態
```http
GET /api/socketio/status
```

## 客戶端示例

### JavaScript客戶端
```javascript
// 連接到Socket.IO服務器
const socket = io('http://localhost:9092');

// 監聽連接事件
socket.on('connect', () => {
    console.log('Connected to server');
});

// 監聽消息事件
socket.on('message', (data) => {
    console.log('Received message:', data);
});

// 發送消息
socket.emit('message', 'Hello Server!');
```

### HTML示例
```html
<!DOCTYPE html>
<html>
<head>
    <title>Socket.IO Test</title>
    <script src="https://cdn.socket.io/4.7.2/socket.io.min.js"></script>
</head>
<body>
    <div id="messages"></div>
    <input type="text" id="messageInput" placeholder="Type a message...">
    <button onclick="sendMessage()">Send</button>

    <script>
        const socket = io('http://localhost:9092');
        
        socket.on('message', (data) => {
            const messages = document.getElementById('messages');
            messages.innerHTML += '<div>' + data + '</div>';
        });
        
        function sendMessage() {
            const input = document.getElementById('messageInput');
            socket.emit('message', input.value);
            input.value = '';
        }
    </script>
</body>
</html>
```

## 運行方式

### 開發模式
```bash
cd socketio
mvn spring-boot:run
```

### 生產模式
```bash
cd socketio
mvn clean package
java -jar target/socketio-0.0.1-SNAPSHOT.jar
```

## 測試

```bash
# 運行測試
mvn test

# 運行特定測試
mvn test -Dtest=SocketIOApplicationTests
```

## 日誌

應用程式會輸出詳細的連接和消息日誌：
- 客戶端連接/斷開事件
- 接收和發送的消息
- 錯誤和調試信息

## 擴展

此模組可以輕鬆擴展以支援：
- 房間(Rooms)和命名空間(Namespaces)
- 用戶認證和授權
- 消息持久化
- 負載均衡
- 集群支援
