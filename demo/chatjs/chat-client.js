/**
 * TinySocket 聊天室 WebSocket 客戶端
 * 處理與後端 ChatWebServer 的通信
 */
class ChatClient {
    constructor() {
        this.ws = null;
        this.isConnected = false;
        this.sessionId = null;
        this.token = null;
        this.requestId = 0;
        this.userId = null;
        this.callbacks = new Map();
        this.eventListeners = new Map();

        // 心跳機制相關
        this.heartbeatInterval = null;
        this.heartbeatTimeout = null;
        this.pingInterval = 30000; // 30秒發送一次心跳
        this.pongTimeout = 10000; // 等待pong響應的超時時間

        // 重連機制相關
        this.isReconnecting = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 2000; // 初始重連延遲2秒
        this.lastUrl = null; // 記錄最後連接的 URL

        // Token 刷新機制相關
        this.tokenRefreshInterval = null;
        // 測試模式：每分鐘更新 Token
        // 正式模式：設為 25 分鐘（25 * 60 * 1000）
        this.isTestMode = true; // 設為 false 切換到正式模式
        this.refreshIntervalTime = this.isTestMode ? 60 * 1000 : 25 * 60 * 1000; // 1分鐘或25分鐘
        this.tokenExpiryTime = 30 * 60 * 1000; // Token 30分鐘過期

        // 協議常數 (與後端 ProtocolId 對應)
        this.PROTOCOLS = {
            PING: { mainNo: 0, subNo: 0 },
            AUTH: { mainNo: 0, subNo: 1 },
            AUTH_RESULT: { mainNo: 0, subNo: 2 },
            REFLASH_TOKEN: { mainNo: 0, subNo: 125 },
            ONLINE: { mainNo: 1, subNo: 1 },
            OFFLINE: { mainNo: 1, subNo: 2 },
            GET_USER_LIST: { mainNo: 1, subNo: 3 },
            GET_USER_INFO: { mainNo: 1, subNo: 4 },
            SAY: { mainNo: 1, subNo: 5 },
            MESSAGE: { mainNo: 1, subNo: 6 }
        };
    }

    /**
     * 連接到 WebSocket 服務器
     */
    connect(url) {
        return new Promise((resolve, reject) => {
            try {
                this.lastUrl = url; // 記錄 URL 用於重連
                this.ws = new WebSocket(url);

                this.ws.onopen = () => {
                    console.log('✅ WebSocket 連接已建立');
                    this.isConnected = true;
                    this.reconnectAttempts = 0; // 重置重連計數
                    this.isReconnecting = false;
                    this.emit('connected');

                    // 連接成功後直接觸發準備認證事件，不再需要單獨請求 sessionId
                    resolve();
                };

                this.ws.onmessage = (event) => {
                    console.log('📨 原始消息數據:', event.data);
                    this.handleMessage(event.data);
                };

                this.ws.onclose = (event) => {
                    console.log('🔌 WebSocket 連接已關閉:', event);
                    this.isConnected = false;
                    this.sessionId = null;
                    this.token = null;
                    this.tokenCreatedTime = null;
                    this.stopHeartbeat(); // 停止心跳
                    this.stopTokenRefresh(); // 停止 Token 刷新
                    this.emit('disconnected', event);

                    // 如果不是手動關閉，嘗試自動重連
                    if (!this.isReconnecting && event.code !== 1000) {
                        this.attemptReconnect();
                    }
                };

                this.ws.onerror = (error) => {
                    console.error('WebSocket 錯誤:', error);
                    this.emit('error', error);
                    reject(error);
                };

            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * 嘗試自動重連
     */
    attemptReconnect() {
        if (this.isReconnecting || this.reconnectAttempts >= this.maxReconnectAttempts || !this.lastUrl) {
            return;
        }

        this.isReconnecting = true;
        this.reconnectAttempts++;

        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1); // 指數退避

        console.log(`🔄 嘗試重連 (${this.reconnectAttempts}/${this.maxReconnectAttempts})，${delay}ms 後重試...`);
        this.emit('reconnecting', { attempt: this.reconnectAttempts, delay });

        setTimeout(async () => {
            try {
                await this.connect(this.lastUrl);
                console.log('✅ 重連成功');
                this.emit('reconnected');
            } catch (error) {
                console.error('❌ 重連失敗:', error);

                if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                    console.error('❌ 達到最大重連次數，停止重連');
                    this.isReconnecting = false;
                    this.emit('reconnect_failed');
                } else {
                    this.isReconnecting = false;
                    this.attemptReconnect();
                }
            }
        }, delay);
    }

    /**
     * 關閉連接
     */
    disconnect() {
        this.isReconnecting = false; // 阻止自動重連
        this.reconnectAttempts = 0;
        this.stopHeartbeat();
        this.stopTokenRefresh(); // 停止 Token 刷新

        if (this.ws && this.isConnected) {
            this.ws.close(1000, 'User disconnected'); // 1000 表示正常關閉
        }
    }

    /**
     * 啟動心跳機制
     */
    startHeartbeat() {
        console.log('💓 啟動心跳機制');
        this.stopHeartbeat(); // 先停止現有的心跳

        this.heartbeatInterval = setInterval(() => {
            if (this.isConnected && this.sessionId) {
                this.sendPing();
            } else {
                console.warn('💓 心跳檢查失敗：未連接或無會話ID');
                this.stopHeartbeat();
                // 移除：不要在心跳失敗時停止 Token 刷新，它們應該獨立運作
            }
        }, this.pingInterval);
    }

    /**
     * 停止心跳機制
     */
    stopHeartbeat() {
        console.log('💓 停止心跳機制');
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
        if (this.heartbeatTimeout) {
            clearTimeout(this.heartbeatTimeout);
            this.heartbeatTimeout = null;
        }
    }

    /**
     * 開始 Token 自動刷新機制
     */
    startTokenRefresh() {
        console.log('🔑 開始 Token 自動刷新機制');

        // 先停止現有的刷新定時器
        this.stopTokenRefresh();

        // 設置定時器，每分鐘嘗試刷新 Token
        this.tokenRefreshInterval = setInterval(() => {
            console.log('🔑 每分鐘定時檢查 Token 刷新...');

            // 只有在連接且有 Token 的情況下才嘗試刷新
            if (this.isConnected && this.token && this.ws && this.ws.readyState === WebSocket.OPEN) {
                // 檢查 Token 是否需要刷新
                if (this.isTokenExpiringSoon()) {
                    console.log('🔑 開始刷新 Token...');
                    this.refreshToken();
                } else {
                    console.log('🔑 Token 仍然有效，跳過刷新');
                }
            } else {
                console.warn('⚠️ Token 刷新跳過：連接狀態不符合條件', {
                    isConnected: this.isConnected,
                    hasToken: !!this.token,
                    wsState: this.ws ? this.ws.readyState : 'null'
                });
            }
        }, this.refreshIntervalTime);

        console.log(`🔑 Token 刷新定時器已設置 (${this.isTestMode ? '測試模式' : '正式模式'})，每 ${this.refreshIntervalTime / 1000} 秒刷新一次`);
    }

    /**
     * 停止 Token 自動刷新機制
     */
    stopTokenRefresh() {
        console.log('🔑 停止 Token 自動刷新機制');
        if (this.tokenRefreshInterval) {
            clearInterval(this.tokenRefreshInterval);
            this.tokenRefreshInterval = null;
        }
    }

    /**
     * 刷新 Token
     */
    refreshToken() {
        // 詳細的狀態檢查
        if (!this.token) {
            console.warn('⚠️ 沒有可用的 Token，無法刷新');
            return false;
        }

        if (!this.isConnected) {
            console.warn('⚠️ WebSocket 未連接，無法刷新 Token');
            return false;
        }

        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.warn('⚠️ WebSocket 連接狀態異常，無法刷新 Token', {
                ws: !!this.ws,
                readyState: this.ws ? this.ws.readyState : 'null'
            });
            return false;
        }

        console.log('🔑 開始刷新 Token...', {
            currentToken: this.token.substring(0, 20) + '...',
            connectionState: this.isConnected,
            wsState: this.ws.readyState
        });

        try {
            // 觸發刷新開始事件
            this.emit('token_refreshing');

            const requestId = ++this.requestId;
            const message = {
                header: {
                    version: "0.0.1",
                    mainNo: this.PROTOCOLS.REFLASH_TOKEN.mainNo,
                    subNo: this.PROTOCOLS.REFLASH_TOKEN.subNo,
                    isCompress: false,
                    sessionId: this.sessionId,
                    requestId: requestId,
                    userId: this.userId || "",
                    token: this.token || ""
                },
                buffer: JSON.stringify({
                    token: this.token
                })
            };

            console.log('🔑 發送 Token 刷新請求:', {
                requestId: requestId,
                protocol: `${message.header.mainNo}.${message.header.subNo}`,
                tokenLength: this.token.length
            });

            // 設置響應回調
            this.callbacks.set(requestId, (error, response) => {
                if (error) {
                    console.error('❌ Token 刷新回調錯誤:', error);
                    this.emit('token_refresh_error', {
                        error: error.message,
                        response: null
                    });
                    return;
                }

                // 處理 Token 刷新響應已在 handleMessage 中統一處理
                console.log('🔑 Token 刷新請求響應已收到');
            });

            const messageStr = JSON.stringify(message);
            this.ws.send(messageStr);
            return true;
        } catch (error) {
            console.error('❌ Token 刷新請求發送失敗:', error);
            this.emit('token_refresh_error', {
                error: '發送請求失敗: ' + error.message,
                response: null
            });
            return false;
        }
    }

    /**
     * 檢查 Token 是否即將過期（前端模擬檢查）
     */
    isTokenExpiringSoon() {
        // 測試模式：總是返回 true，讓後端認為 Token 即將過期
        if (this.isTestMode) {
            console.log('🔑 測試模式：強制認為 Token 即將過期');
            return true;
        }

        // 正式模式：實際檢查 Token 年齡
        // 實際項目中可以解析 JWT Token 來獲取精確的過期時間
        if (!this.tokenCreatedTime) {
            return false;
        }

        const now = Date.now();
        const tokenAge = now - this.tokenCreatedTime;
        const expiryThreshold = this.tokenExpiryTime - (5 * 60 * 1000); // 提前5分鐘檢查

        return tokenAge >= expiryThreshold;
    }

    /**
     * 發送心跳 ping
     */
    sendPing() {
        console.log('💓 發送心跳 ping');

        // 發送心跳開始事件
        this.emit('heartbeat_sending');

        // 記錄發送時間
        const startTime = Date.now();

        const requestId = ++this.requestId;
        const message = {
            header: {
                version: "0.0.1",
                mainNo: this.PROTOCOLS.PING.mainNo,
                subNo: this.PROTOCOLS.PING.subNo,
                isCompress: false,
                sessionId: this.sessionId,
                requestId: requestId,
                userId: this.userId || "",
                token: this.token || ""
            },
            buffer: JSON.stringify({ ping: "ping" })  // 將 buffer 序列化為 JSON 字符串
        };

        // 設置 pong 響應回調
        this.callbacks.set(requestId, (error, response) => {
            if (this.heartbeatTimeout) {
                clearTimeout(this.heartbeatTimeout);
                this.heartbeatTimeout = null;
            }

            if (error) {
                console.error('💓 心跳失敗:', error);
                this.emit('heartbeat_error', error);
            } else {
                // 計算響應時間
                const responseTime = Date.now() - startTime;
                console.log(`💓 收到 pong 響應: ${responseTime}ms`, response);
                this.emit('heartbeat_success', {
                    response: response,
                    responseTime: responseTime
                });
            }
        });

        // 設置 pong 響應超時
        this.heartbeatTimeout = setTimeout(() => {
            if (this.callbacks.has(requestId)) {
                this.callbacks.delete(requestId);
                console.error('💓 心跳超時：未收到 pong 響應');
                this.emit('heartbeat_timeout');

                // 心跳超時，可能需要重新連接
                this.disconnect();
                this.emit('connection_lost');
            }
        }, this.pongTimeout);

        const messageStr = JSON.stringify(message);
        console.log('💓 發送心跳消息:', messageStr);
        this.ws.send(messageStr);
    }

    /**
     * 處理接收到的消息
     */
    handleMessage(data) {
        try {
            const message = JSON.parse(data);
            console.log('📨 解析後的消息:', message);

            // 處理認證結果響應 (AUTH_RESULT: mainNo=0, subNo=2)
            if (message.header &&
                message.header.mainNo === this.PROTOCOLS.AUTH_RESULT.mainNo &&
                message.header.subNo === this.PROTOCOLS.AUTH_RESULT.subNo) {
                console.log('🔐 收到認證結果響應:', message);
                this.handleAuthResult(message);
                return;
            }

            // 處理 Token 刷新響應 (REFLASH_TOKEN: mainNo=0, subNo=125)
            if (message.header &&
                message.header.mainNo === this.PROTOCOLS.REFLASH_TOKEN.mainNo &&
                message.header.subNo === this.PROTOCOLS.REFLASH_TOKEN.subNo) {
                console.log('🔑 收到 Token 刷新響應:', message);
                this.handleTokenRefreshResult(message);
                return;
            }

            // 處理協議響應 (有 requestId 的響應)
            if (message.header && message.header.requestId) {
                const callback = this.callbacks.get(message.header.requestId);
                if (callback) {
                    this.callbacks.delete(message.header.requestId);
                    callback(null, message);
                    return;
                }
            }

            // 處理廣播消息
            this.handleBroadcast(message);

            // 如果沒有處理，記錄未知消息
            console.log('❓ 未處理的消息:', message);

        } catch (error) {
            console.error('❌ 解析消息時出錯:', error, '原始數據:', data);
        }
    }

    /**
     * 處理認證結果響應
     */
    handleAuthResult(message) {
        console.log('🔍 處理認證結果:', message);

        // 檢查響應結構
        let responseData = null;
        if (message.buffer) {
            // 響應的 buffer 是 JSON 字符串，需要解析
            try {
                responseData = JSON.parse(message.buffer);
            } catch (e) {
                console.error('解析認證響應 buffer 失敗:', e);
                responseData = message.buffer;
            }
        } else if (message.data) {
            responseData = message.data;
        } else {
            // 直接檢查 message 本身
            responseData = message;
        }

        console.log('🔍 解析後的認證響應數據:', responseData);

        // 檢查是否有 requestId，如果有就使用正常的回調處理
        if (message.header && message.header.requestId) {
            const callback = this.callbacks.get(message.header.requestId);
            if (callback) {
                this.callbacks.delete(message.header.requestId);

                if (responseData && responseData.code === 200) {
                    // 認證成功，提取 sessionId 和 token
                    this.sessionId = responseData.sessionId;
                    this.token = responseData.token;
                    this.tokenCreatedTime = Date.now(); // 記錄 Token 創建時間

                    console.log('✅ 認證成功，獲得 sessionId:', this.sessionId);
                    console.log('✅ 認證成功，獲得 token:', this.token);

                    // 啟動心跳機制
                    this.startHeartbeat();

                    // 啟動 Token 自動刷新機制
                    this.startTokenRefresh();

                    this.emit('authenticated', {
                        sessionId: this.sessionId,
                        token: this.token
                    });

                    callback(null, { buffer: JSON.stringify(responseData) });
                } else if (responseData && responseData.code) {
                    callback(new Error(responseData.message || `認證失敗，錯誤代碼: ${responseData.code}`));
                } else {
                    console.error('❌ 無法解析的認證響應格式:', message);
                    callback(new Error('認證響應格式錯誤，無法解析'));
                }
                return;
            }
        }

        // 如果沒有 requestId 或找不到對應回調，處理無 requestId 的認證響應
        // 這種情況下可能是服務器主動發送的認證結果
        if (responseData && responseData.code === 200) {
            // 認證成功，提取 sessionId 和 token
            this.sessionId = responseData.sessionId;
            this.token = responseData.token;

            console.log('✅ 認證成功（無請求ID），獲得 sessionId:', this.sessionId);
            console.log('✅ 認證成功（無請求ID），獲得 token:', this.token);

            // 啟動心跳機制
            this.startHeartbeat();

            this.emit('authenticated', {
                sessionId: this.sessionId,
                token: this.token
            });
        } else {
            console.warn('⚠️ 沒有找到對應的認證回調，且認證響應無效:', responseData);
        }
    }

    /**
     * 處理 Token 刷新結果響應
     */
    handleTokenRefreshResult(message) {
        console.log('🔍 處理 Token 刷新結果:', message);

        // 檢查響應結構
        let responseData = null;
        if (message.buffer) {
            try {
                responseData = JSON.parse(message.buffer);
            } catch (e) {
                console.error('解析 Token 刷新響應 buffer 失敗:', e);
                responseData = message.buffer;
            }
        } else if (message.data) {
            responseData = message.data;
        } else {
            responseData = message;
        }

        console.log('🔍 解析後的 Token 刷新響應數據:', responseData);

        if (responseData && responseData.code === 200) {
            // Token 刷新成功
            const oldToken = this.token;
            this.token = responseData.token;
            this.tokenCreatedTime = Date.now(); // 更新 Token 創建時間

            console.log('✅ Token 刷新成功');
            console.log('🔑 舊 Token:', oldToken);
            console.log('🔑 新 Token:', this.token);

            // 觸發 Token 刷新成功事件
            this.emit('token_refreshed', {
                oldToken: oldToken,
                newToken: this.token
            });
        } else {
            // Token 刷新失敗
            const errorMessage = responseData ? responseData.message || `Token 刷新失敗，錯誤代碼: ${responseData.code}` : 'Token 刷新響應格式錯誤';
            console.error('❌ Token 刷新失敗:', errorMessage);

            // 觸發 Token 刷新失敗事件
            this.emit('token_refresh_error', {
                error: errorMessage,
                response: responseData
            });

            // 如果 Token 刷新失敗，可能需要重新認證
            // 這裡可以根據具體的錯誤代碼決定是否需要重新登入
            if (responseData && (responseData.code === 401 || responseData.code === 403)) {
                console.warn('⚠️ Token 刷新失敗，可能需要重新認證');
                this.stopTokenRefresh();
                this.emit('token_expired');
            }
        }
    }

    /**
     * 處理廣播消息
     */
    handleBroadcast(message) {
        if (message.header) {
            // 檢查是否為 MESSAGE 協議廣播 (mainNo=1, subNo=6)
            if (message.header.mainNo === 1 && message.header.subNo === 6) {
                console.log('📨 處理 MESSAGE 廣播消息:', message);

                // 解析消息數據
                let messageData = null;
                if (message.buffer) {
                    try {
                        // buffer 是 JSON 字符串，需要解析
                        if (typeof message.buffer === 'string') {
                            const parsedBuffer = JSON.parse(message.buffer);
                            // 檢查是否有嵌套的 message 字段
                            if (parsedBuffer.message) {
                                messageData = parsedBuffer.message;
                            } else {
                                messageData = parsedBuffer;
                            }
                        } else {
                            messageData = message.buffer;
                        }
                    } catch (e) {
                        console.error('解析廣播消息 buffer 失敗:', e);
                        messageData = message.buffer;
                    }
                } else if (message.data) {
                    messageData = message.data;
                } else {
                    messageData = message;
                }

                console.log('📨 解析後的廣播消息數據:', messageData);

                // 只有當消息數據有效且有內容時才觸發事件
                if (messageData && messageData.content) {
                    this.emit('message', messageData);
                } else {
                    console.warn('廣播消息數據無效或缺少內容，已跳過:', messageData);
                }
            }
        }
    }

    /**
     * 發送消息 - 現在需要包含 token
     */
    sendMessage(protocol, data, callback) {
        // 驗證連接狀態
        if (!this.isConnected) {
            const error = new Error('WebSocket 未連接');
            console.error('❌ 發送消息失敗:', error.message);
            if (callback) callback(error);
            return;
        }

        if (!this.sessionId) {
            const error = new Error('未獲得會話ID，請先進行認證');
            console.error('❌ 發送消息失敗:', error.message);
            if (callback) callback(error);
            return;
        }

        // 驗證協議參數
        if (!protocol || typeof protocol.mainNo !== 'number' || typeof protocol.subNo !== 'number') {
            const error = new Error('無效的協議參數');
            console.error('❌ 發送消息失敗:', error.message, protocol);
            if (callback) callback(error);
            return;
        }

        const requestId = ++this.requestId;

        try {
            const message = {
                header: {
                    version: "0.0.1",
                    mainNo: protocol.mainNo,
                    subNo: protocol.subNo,
                    isCompress: false,
                    sessionId: this.sessionId,
                    requestId: requestId,
                    userId: this.userId || "",
                    token: this.token || ""
                },
                buffer: JSON.stringify(data || {})  // 將 buffer 序列化為 JSON 字符串
            };

            if (callback) {
                this.callbacks.set(requestId, callback);
                // 設置超時
                setTimeout(() => {
                    if (this.callbacks.has(requestId)) {
                        this.callbacks.delete(requestId);
                        callback(new Error('請求超時'));
                    }
                }, 10000);
            }

            const messageStr = JSON.stringify(message);
            console.log('📤 發送消息:', messageStr);
            this.ws.send(messageStr);

        } catch (error) {
            console.error('❌ 發送消息時出錯:', error);
            if (callback) {
                // 清理回調
                if (this.callbacks.has(requestId)) {
                    this.callbacks.delete(requestId);
                }
                callback(error);
            }
        }
    }

    /**
     * 用戶認證 - 認證成功後會同時獲得 sessionId 和 token
     */
    authenticate(userId, password) {
        return new Promise((resolve, reject) => {
            this.userId = userId;
            const requestId = ++this.requestId;

            const data = {
                userId: userId,
                password: password
            };

            // 創建認證消息，使用臨時 sessionId 0
            const message = {
                header: {
                    version: "0.0.1",
                    mainNo: this.PROTOCOLS.AUTH.mainNo,
                    subNo: this.PROTOCOLS.AUTH.subNo,
                    isCompress: false,
                    sessionId: 0, // 使用臨時 sessionId，認證成功後會獲得真實的 sessionId
                    requestId: requestId,
                    userId: userId,
                    token: ""
                },
                buffer: JSON.stringify(data)  // 將 buffer 序列化為 JSON 字符串
            };

            // 設置回調來處理認證響應
            this.callbacks.set(requestId, (error, response) => {
                if (error) {
                    reject(error);
                } else {
                    resolve(response);
                }
            });

            const messageStr = JSON.stringify(message);
            console.log('📤 發送認證請求:', messageStr);
            this.ws.send(messageStr);
        });
    }

    /**
     * 用戶上線
     */
    goOnline() {
        return new Promise((resolve, reject) => {
            this.sendMessage(this.PROTOCOLS.ONLINE, {}, (error, response) => {
                if (error) {
                    reject(error);
                } else {
                    resolve(response);
                }
            });
        });
    }

    /**
     * 用戶下線
     */
    goOffline() {
        return new Promise((resolve, reject) => {
            this.sendMessage(this.PROTOCOLS.OFFLINE, {}, (error, response) => {
                if (error) {
                    reject(error);
                } else {
                    resolve(response);
                }
            });
        });
    }

    /**
     * 獲取用戶列表
     */
    getUserList() {
        return new Promise((resolve, reject) => {
            this.sendMessage(this.PROTOCOLS.GET_USER_LIST, {}, (error, response) => {
                if (error) {
                    reject(error);
                } else {
                    resolve(response);
                }
            });
        });
    }

    /**
     * 獲取指定用戶的詳細資訊
     */
    getUserInfo(targetId) {
        return new Promise((resolve, reject) => {
            if (!targetId) {
                reject(new Error('用戶ID不能為空'));
                return;
            }

            const data = {
                targetId: targetId
            };

            console.log(`🔍 獲取用戶資訊: ${targetId}`);
            this.sendMessage(this.PROTOCOLS.GET_USER_INFO, data, (error, response) => {
                if (error) {
                    console.error('❌ 獲取用戶資訊失敗:', error);
                    reject(error);
                } else {
                    console.log('✅ 用戶資訊獲取成功:', response);
                    resolve(response);
                }
            });
        });
    }

    /**
     * 發送聊天消息
     */
    sendChatMessage(content) {
        return new Promise((resolve, reject) => {
            const data = {
                content: content
            };

            this.sendMessage(this.PROTOCOLS.SAY, data, (error, response) => {
                if (error) {
                    reject(error);
                } else {
                    resolve(response);
                }
            });
        });
    }

    /**
     * 事件監聽
     */
    on(event, callback) {
        if (!this.eventListeners.has(event)) {
            this.eventListeners.set(event, []);
        }
        this.eventListeners.get(event).push(callback);
    }

    /**
     * 移除事件監聽
     */
    off(event, callback) {
        if (this.eventListeners.has(event)) {
            const listeners = this.eventListeners.get(event);
            const index = listeners.indexOf(callback);
            if (index > -1) {
                listeners.splice(index, 1);
            }
        }
    }

    /**
     * 觸發事件
     */
    emit(event, data) {
        if (this.eventListeners.has(event)) {
            this.eventListeners.get(event).forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error('事件回調執行錯誤:', error);
                }
            });
        }
    }

    /**
     * 獲取連接狀態
     */
    getConnectionState() {
        if (!this.ws) return 'disconnected';

        switch (this.ws.readyState) {
            case WebSocket.CONNECTING:
                return 'connecting';
            case WebSocket.OPEN:
                return 'connected';
            case WebSocket.CLOSING:
                return 'disconnecting';
            case WebSocket.CLOSED:
                return 'disconnected';
            default:
                return 'unknown';
        }
    }
}

// 導出 ChatClient 類
window.ChatClient = ChatClient;
