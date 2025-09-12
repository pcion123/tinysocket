/**
 * TinySocket 聊天室應用程序主邏輯
 */
class ChatApp {
    constructor() {
        console.log('🏗️ 創建 ChatApp 實例...');

        this.client = new ChatClient();
        this.currentUser = null;
        this.onlineUsers = [];
        this.messages = [];
        this.isLoggedIn = false;

        try {
            this.initializeElements();
            console.log('✅ DOM 元素初始化完成');

            this.bindEvents();
            console.log('✅ 事件綁定完成');

            this.setupClientEvents();
            console.log('✅ 客戶端事件設置完成');

            this.initializeTheme();
            console.log('✅ 主題初始化完成');
        } catch (error) {
            console.error('❌ ChatApp 初始化錯誤:', error);
        }
    }

    /**
     * 初始化 DOM 元素
     */
    initializeElements() {
        console.log('🔍 開始初始化 DOM 元素...');

        // 登入界面元素
        this.loginContainer = document.getElementById('loginContainer');
        console.log('🔍 loginContainer:', this.loginContainer);

        this.userIdInput = document.getElementById('userId');
        console.log('🔍 userIdInput:', this.userIdInput);

        this.passwordInput = document.getElementById('password');
        console.log('🔍 passwordInput:', this.passwordInput);

        this.serverUrlInput = document.getElementById('serverUrl');
        console.log('🔍 serverUrlInput:', this.serverUrlInput);

        this.connectBtn = document.getElementById('connectBtn');
        console.log('🔍 connectBtn:', this.connectBtn);

        this.loginStatus = document.getElementById('loginStatus');
        console.log('🔍 loginStatus:', this.loginStatus);

        this.loginThemeToggleBtn = document.getElementById('loginThemeToggleBtn');
        console.log('🔍 loginThemeToggleBtn:', this.loginThemeToggleBtn);

        // 聊天界面元素
        this.chatContainer = document.getElementById('chatContainer');
        console.log('🔍 chatContainer:', this.chatContainer);

        this.connectionStatus = document.getElementById('connectionStatus');
        console.log('🔍 connectionStatus:', this.connectionStatus);

        this.heartbeatStatus = document.getElementById('heartbeatStatus');
        console.log('🔍 heartbeatStatus:', this.heartbeatStatus);

        this.currentUserSpan = document.getElementById('currentUser');
        console.log('🔍 currentUserSpan:', this.currentUserSpan);

        this.disconnectBtn = document.getElementById('disconnectBtn');
        console.log('🔍 disconnectBtn:', this.disconnectBtn);

        this.themeToggleBtn = document.getElementById('themeToggleBtn');
        console.log('🔍 themeToggleBtn:', this.themeToggleBtn);

        this.usersList = document.getElementById('usersList');
        console.log('🔍 usersList:', this.usersList);

        this.userCount = document.getElementById('userCount');
        console.log('🔍 userCount:', this.userCount);

        this.refreshUsersBtn = document.getElementById('refreshUsersBtn');
        console.log('🔍 refreshUsersBtn:', this.refreshUsersBtn);

        this.messagesContainer = document.getElementById('messagesContainer');
        console.log('🔍 messagesContainer:', this.messagesContainer);

        this.messageInput = document.getElementById('messageInput');
        console.log('🔍 messageInput:', this.messageInput);

        this.sendBtn = document.getElementById('sendBtn');
        console.log('🔍 sendBtn:', this.sendBtn);

        this.charCount = document.getElementById('charCount');
        console.log('🔍 charCount:', this.charCount);

        this.notification = document.getElementById('notification');
        console.log('🔍 notification:', this.notification);

        this.loadingOverlay = document.getElementById('loadingOverlay');
        console.log('🔍 loadingOverlay:', this.loadingOverlay);

        // 用戶資訊彈出框元素
        this.userInfoModal = document.getElementById('userInfoModal');
        console.log('🔍 userInfoModal:', this.userInfoModal);

        this.closeUserInfoModal = document.getElementById('closeUserInfoModal');
        console.log('🔍 closeUserInfoModal:', this.closeUserInfoModal);

        this.userInfoContent = document.getElementById('userInfoContent');
        console.log('🔍 userInfoContent:', this.userInfoContent);

        // 檢查關鍵元素
        if (!this.connectBtn) {
            console.error('❌ 找不到連接按鈕元素');
        } else {
            console.log('✅ 連接按鈕元素找到');
        }

        if (!this.heartbeatStatus) {
            console.error('❌ 找不到心跳狀態元素');
        } else {
            console.log('✅ 心跳狀態元素找到');
        }
    }

    /**
     * 綁定事件監聽器
     */
    bindEvents() {
        console.log('🔗 開始綁定事件監聽器...');

        // 登入相關事件
        if (this.connectBtn) {
            this.connectBtn.addEventListener('click', () => {
                console.log('🖱️ 連接按鈕被點擊');
                this.handleConnect();
            });
            console.log('✅ 連接按鈕事件綁定完成');
        } else {
            console.error('❌ 無法綁定連接按鈕事件 - 元素不存在');
        }

        if (this.passwordInput) {
            this.passwordInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    console.log('⌨️ 密碼框回車鍵被按下');
                    this.handleConnect();
                }
            });
        }

        // 登入頁面主題切換事件
        if (this.loginThemeToggleBtn) {
            this.loginThemeToggleBtn.addEventListener('click', () => this.toggleTheme());
        }

        // 聊天相關事件
        if (this.disconnectBtn) {
            this.disconnectBtn.addEventListener('click', () => this.handleDisconnect());
        }
        if (this.themeToggleBtn) {
            this.themeToggleBtn.addEventListener('click', () => this.toggleTheme());
        }
        if (this.refreshUsersBtn) {
            this.refreshUsersBtn.addEventListener('click', () => this.refreshUserList());
        }
        if (this.sendBtn) {
            this.sendBtn.addEventListener('click', () => this.sendMessage());
        }
        if (this.messageInput) {
            this.messageInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            });

            // 字數統計
            this.messageInput.addEventListener('input', () => {
                this.updateCharCount();
            });
        }

        // 用戶資訊彈出框相關事件
        if (this.closeUserInfoModal) {
            this.closeUserInfoModal.addEventListener('click', () => {
                this.closeUserInfo();
            });
        }

        // 點擊彈出框背景關閉
        if (this.userInfoModal) {
            this.userInfoModal.addEventListener('click', (e) => {
                if (e.target === this.userInfoModal) {
                    this.closeUserInfo();
                }
            });
        }
    }

    /**
     * 設置客戶端事件
     */
    setupClientEvents() {
        this.client.on('connected', () => {
            this.showLoginStatus('WebSocket 連接成功，準備開始認證...', 'success');
            this.authenticateUser();
        });

        this.client.on('authenticated', (authData) => {
            this.showLoginStatus('認證成功，進入聊天室...', 'success');
            this.hideLoading();

            // 延遲一點進入聊天室，讓用戶看到成功消息
            setTimeout(() => {
                this.enterChatRoom();
                // 進入聊天室後顯示心跳和 Token 狀態
                this.showHeartbeatStatus();
                this.showTokenStatus();
            }, 1000);
        });

        this.client.on('disconnected', () => {
            this.updateConnectionStatus(false);
            if (this.isLoggedIn) {
                this.showNotification('連接已斷開', 'error');
                this.returnToLogin();
            }
        });

        this.client.on('error', (error) => {
            this.hideLoading();
            this.showLoginStatus('操作失敗: ' + error.message, 'error');
        });

        this.client.on('message', (messageData) => {
            this.handleIncomingMessage(messageData);
        });

        // 心跳事件處理
        this.client.on('heartbeat_sending', () => {
            this.updateHeartbeatStatus('sending');
        });

        this.client.on('heartbeat_success', (data) => {
            const responseTime = data.responseTime || 0;
            this.updateHeartbeatStatus('success', responseTime);
        });

        this.client.on('heartbeat_error', (error) => {
            this.updateHeartbeatStatus('error');
        });

        this.client.on('heartbeat_timeout', () => {
            this.updateHeartbeatStatus('timeout');
            this.showNotification('連接心跳超時', 'warning');
        });

        this.client.on('connection_lost', () => {
            this.updateHeartbeatStatus('error', 0, '斷線');
            this.showNotification('連接丟失，正在重新連接...', 'error');
            if (this.isLoggedIn) {
                this.returnToLogin();
            }
        });

        // Token 刷新事件處理
        this.client.on('token_refreshing', () => {
            this.updateTokenStatus('refreshing');
            this.showNotification('正在刷新 Token...', 'info');
        });

        this.client.on('token_refreshed', (data) => {
            this.updateTokenStatus('refreshed');
            this.showNotification('Token 刷新成功', 'success');
            console.log('🔑 Token 已更新:', data.newToken);
        });

        this.client.on('token_refresh_error', (error) => {
            this.updateTokenStatus('error');
            this.showNotification('Token 刷新失敗: ' + error.error, 'error');
            console.error('❌ Token 刷新失敗:', error);
        });

        this.client.on('token_expired', () => {
            this.updateTokenStatus('expired');
            this.showNotification('Token 已過期，請重新登入', 'warning');
            // 自動返回登入畫面
            this.returnToLogin();
        });
    }

    /**
     * 處理連接按鈕點擊
     */
    async handleConnect() {
        console.log('🔄 handleConnect 被調用');

        const userId = this.userIdInput.value.trim();
        const password = this.passwordInput.value.trim();
        const serverUrl = this.serverUrlInput.value.trim();

        console.log('📋 表單數據:', { userId, password, serverUrl });

        if (!userId || !password || !serverUrl) {
            this.showLoginStatus('請填寫所有欄位', 'error');
            return;
        }

        this.currentUser = userId;

        if (this.connectBtn) {
            this.connectBtn.disabled = true;
        } else {
            console.error('❌ connectBtn 元素不存在');
        }

        this.showLoading();

        try {
            await this.client.connect(serverUrl);
            // 連接成功後，會在 connected 事件中直接開始認證
        } catch (error) {
            this.hideLoading();
            if (this.connectBtn) {
                this.connectBtn.disabled = false;
            }
            this.showLoginStatus('連接失敗: ' + error.message, 'error');
        }
    }

    /**
     * 認證用戶
     */
    async authenticateUser() {
        try {
            const response = await this.client.authenticate(
                this.userIdInput.value.trim(),
                this.passwordInput.value.trim()
            );

            console.log('認證響應:', response);
            // 認證成功的處理已經在 authenticated 事件中完成

        } catch (error) {
            console.error('認證失敗:', error);
            this.connectBtn.disabled = false;
            this.hideLoading();
            this.showLoginStatus('認證失敗: ' + error.message, 'error');
            this.client.disconnect();
        }
    }

    /**
     * 進入聊天室
     */
    async enterChatRoom() {
        try {
            // 用戶上線
            const onlineResponse = await this.client.goOnline();
            console.log('上線響應:', onlineResponse);

            // 處理歷史消息
            let responseData = null;
            if (onlineResponse.buffer) {
                // 響應的 buffer 是 JSON 字符串，需要解析
                try {
                    responseData = JSON.parse(onlineResponse.buffer);
                } catch (e) {
                    console.error('解析上線響應 buffer 失敗:', e);
                    responseData = onlineResponse.buffer;
                }
            } else if (onlineResponse.data) {
                responseData = onlineResponse.data;
            } else {
                responseData = onlineResponse;
            }

            console.log('解析後的上線響應數據:', responseData);

            if (responseData && responseData.recentMessages) {
                console.log('載入歷史消息:', responseData.recentMessages);
                this.loadRecentMessages(responseData.recentMessages);
            } else {
                console.log('沒有歷史消息需要載入');
            }

            // 獲取用戶列表
            await this.refreshUserList();

            // 切換到聊天界面
            this.loginContainer.style.display = 'none';
            this.chatContainer.style.display = 'flex';
            this.currentUserSpan.textContent = `當前用戶: ${this.currentUser}`;
            this.updateConnectionStatus(true);
            this.isLoggedIn = true;

            // 聚焦到消息輸入框
            this.messageInput.focus();

            this.showNotification('歡迎進入聊天室！', 'success');

        } catch (error) {
            console.error('進入聊天室失敗:', error);
            this.showLoginStatus('進入聊天室失敗: ' + error.message, 'error');
            this.connectBtn.disabled = false;
            this.client.disconnect();
        }
    }

    /**
     * 處理斷開連接
     */
    async handleDisconnect() {
        console.log('🔄 處理斷開連接');
        try {
            if (this.client.isConnected) {
                await this.client.goOffline();
            }
        } catch (error) {
            console.error('下線失敗:', error);
        } finally {
            this.client.disconnect();
            this.returnToLogin();
        }
    }

    /**
     * 返回登入界面
     */
    returnToLogin() {
        this.isLoggedIn = false;
        this.chatContainer.style.display = 'none';
        this.loginContainer.style.display = 'flex';
        this.connectBtn.disabled = false;
        this.updateHeartbeatStatus('hidden'); // 隱藏心跳狀態
        this.updateTokenStatus('hidden'); // 隱藏 Token 狀態
        this.clearChat();
        this.showLoginStatus('', '');
    }

    /**
     * 發送消息
     */
    async sendMessage() {
        const content = this.messageInput.value.trim();
        if (!content) {
            this.showNotification('請輸入消息內容', 'warning');
            return;
        }

        // 檢查連接狀態
        if (!this.client.isConnected) {
            console.error('客戶端未連接');
            this.showNotification('連接已斷開，請重新連接', 'error');
            return;
        }

        // 檢查會話狀態
        if (!this.client.sessionId) {
            console.error('未獲得會話ID');
            this.showNotification('會話無效，請重新登入', 'error');
            this.returnToLogin();
            return;
        }

        // 檢查消息長度
        if (content.length > 500) {
            this.showNotification('消息內容過長，請控制在500字以內', 'warning');
            return;
        }

        console.log('📤 準備發送消息:', content);

        this.sendBtn.disabled = true;
        this.messageInput.disabled = true;

        try {
            const response = await this.client.sendChatMessage(content);
            console.log('📤 發送消息響應:', response);

            // 清空輸入框
            this.messageInput.value = '';
            this.updateCharCount();

            console.log('✅ 消息發送成功');
        } catch (error) {
            console.error('❌ 發送消息失敗:', error);
            this.showNotification('發送消息失敗: ' + error.message, 'error');
        } finally {
            this.sendBtn.disabled = false;
            this.messageInput.disabled = false;
            this.messageInput.focus();
        }
    }

    /**
     * 更新字符計數
     */
    updateCharCount() {
        if (this.messageInput && this.charCount) {
            const currentLength = this.messageInput.value.length;
            this.charCount.textContent = `${currentLength}/500`;

            // 根據字符數改變顏色
            if (currentLength > 450) {
                this.charCount.style.color = '#ef4444'; // 紅色警告
            } else if (currentLength > 350) {
                this.charCount.style.color = '#f59e0b'; // 橙色提醒
            } else {
                this.charCount.style.color = '#6b7280'; // 正常灰色
            }
        }
    }

    /**
     * 刷新用戶列表
     */
    async refreshUserList() {
        console.log('👥 準備刷新用戶列表');

        if (!this.client.isConnected) {
            console.error('客戶端未連接');
            this.showNotification('未連接到服務器', 'error');
            return;
        }

        if (!this.client.sessionId) {
            console.error('未獲得會話ID');
            this.showNotification('未獲得會話ID', 'error');
            return;
        }

        try {
            const response = await this.client.getUserList();
            console.log('👥 獲取用戶列表響應:', response);

            // 解析響應數據
            let responseData = null;
            if (response.data) {
                responseData = response.data;
            } else if (response.buffer) {
                try {
                    responseData = JSON.parse(response.buffer);
                } catch (e) {
                    console.error('解析用戶列表響應失敗:', e);
                    responseData = response;
                }
            } else {
                responseData = response;
            }

            console.log('👥 解析後的用戶列表數據:', responseData);

            if (responseData && responseData.users) {
                this.updateUsersList(responseData.users);
            } else {
                console.warn('用戶列表響應格式不正確:', responseData);
                this.showNotification('用戶列表格式錯誤', 'error');
            }
        } catch (error) {
            console.error('獲取用戶列表失敗:', error);
            this.showNotification('獲取用戶列表失敗', 'error');
        }
    }

    /**
     * 更新用戶列表
     */
    updateUsersList(users) {
        this.onlineUsers = users;

        if (this.userCount) {
            this.userCount.textContent = users.length;
        } else {
            console.error('❌ userCount 元素不存在');
        }

        if (this.usersList) {
            this.usersList.innerHTML = '';
            users.forEach(user => {
                const userElement = this.createUserElement(user);
                this.usersList.appendChild(userElement);
            });
        } else {
            console.error('❌ usersList 元素不存在');
        }
    }

    /**
     * 創建用戶元素
     */
    createUserElement(user) {
        const userDiv = document.createElement('div');
        userDiv.className = 'user-item';
        userDiv.style.cursor = 'pointer';

        // 添加點擊事件
        userDiv.addEventListener('click', () => {
            console.log(`👆 用戶項目被點擊: ${user.userId}`);
            this.showUserInfo(user.userId);
        });

        // 添加 hover 效果
        userDiv.addEventListener('mouseenter', () => {
            userDiv.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
        });

        userDiv.addEventListener('mouseleave', () => {
            userDiv.style.backgroundColor = '';
        });

        const avatar = document.createElement('div');
        avatar.className = 'user-avatar';
        avatar.textContent = user.userName ? user.userName.charAt(0).toUpperCase() : 'U';

        const userInfo = document.createElement('div');
        userInfo.className = 'user-info';

        const userName = document.createElement('div');
        userName.className = 'user-name';
        userName.textContent = user.userName || user.userId || '未知用戶';

        const userStatus = document.createElement('div');
        userStatus.className = 'user-status';
        userStatus.innerHTML = `
            <i class="fas fa-circle" style="color: #22c55e; font-size: 8px; margin-right: 5px;"></i>
            在線
        `;

        userInfo.appendChild(userName);
        userInfo.appendChild(userStatus);
        userDiv.appendChild(avatar);
        userDiv.appendChild(userInfo);

        return userDiv;
    }

    /**
     * 載入歷史消息
     */
    loadRecentMessages(messages) {
        if (Array.isArray(messages)) {
            console.log(`載入 ${messages.length} 條歷史消息`);
            messages.forEach((message, index) => {
                console.log(`消息 ${index}:`, message);
                // 檢查消息是否有效
                if (message && (message.content || message.messageType !== undefined)) {
                    this.addMessageToUI(message);
                } else {
                    console.warn(`跳過無效消息 ${index}:`, message);
                }
            });
        } else {
            console.warn('歷史消息不是數組格式:', messages);
        }
    }

    /**
     * 處理接收到的消息
     */
    handleIncomingMessage(messageData) {
        console.log('收到廣播消息:', messageData);
        this.addMessageToUI(messageData);
    }

    /**
     * 添加消息到 UI
     */
    addMessageToUI(messageData) {
        if (!messageData) {
            console.warn('嘗試添加空的消息數據');
            return;
        }

        console.log('添加消息到 UI:', messageData);
        const messageElement = this.createMessageElement(messageData);

        // 只有成功創建消息元素才添加到 UI
        if (messageElement && this.messagesContainer) {
            this.messagesContainer.appendChild(messageElement);
            this.scrollToBottom();
        } else if (!messageElement) {
            console.log('消息元素創建失敗，已跳過');
        }
    }

    /**
     * 創建消息元素
     */
    createMessageElement(messageData) {
        // 檢查消息數據是否有效
        if (!messageData) {
            console.warn('消息數據為空，跳過創建');
            return null;
        }

        // 檢查必要的字段
        if (typeof messageData !== 'object') {
            console.warn('消息數據不是對象，跳過創建:', messageData);
            return null;
        }

        // 檢查消息內容是否存在且不為空
        if (!messageData.content || typeof messageData.content !== 'string' || messageData.content.trim() === '') {
            console.warn('消息內容為空或無效，跳過創建:', messageData);
            return null;
        }

        // 確保 messageType 是數字
        const messageType = typeof messageData.messageType === 'number' ? messageData.messageType : 1;

        const messageDiv = document.createElement('div');
        messageDiv.className = 'message';

        if (messageType === 0) {
            // 系統消息
            messageDiv.classList.add('system');

            const content = document.createElement('div');
            content.className = 'message-content';
            content.textContent = messageData.content;

            messageDiv.appendChild(content);
        } else {
            // 用戶消息
            const isCurrentUser = messageData.userId === this.currentUser;
            messageDiv.classList.add(isCurrentUser ? 'current-user' : 'user');

            if (!isCurrentUser) {
                const header = document.createElement('div');
                header.className = 'message-header';

                const avatar = document.createElement('div');
                avatar.className = 'message-avatar';
                avatar.textContent = messageData.userName ? messageData.userName.charAt(0).toUpperCase() : 'U';

                const userName = document.createElement('span');
                userName.textContent = messageData.userName || messageData.userId;

                const time = document.createElement('span');
                time.className = 'message-time';
                time.textContent = this.formatTime(messageData.timestamp);

                header.appendChild(avatar);
                header.appendChild(userName);
                header.appendChild(time);
                messageDiv.appendChild(header);
            }

            const content = document.createElement('div');
            content.className = 'message-content';
            content.textContent = messageData.content;

            messageDiv.appendChild(content);
        }

        return messageDiv;
    }

    /**
     * 格式化時間
     */
    formatTime(timestamp) {
        if (!timestamp) return '';

        try {
            let date;
            if (typeof timestamp === 'string') {
                // 處理後端返回的時間格式 "2025-09-12-00:10:19.551514400"
                if (timestamp.includes('-') && timestamp.includes(':')) {
                    // 將格式轉換為標準 ISO 格式
                    const isoFormat = timestamp.replace(/^(\d{4})-(\d{2})-(\d{2})-/, '$1-$2-$3T');
                    date = new Date(isoFormat);
                } else {
                    date = new Date(timestamp);
                }
            } else if (Array.isArray(timestamp)) {
                // LocalDateTime 數組格式 [year, month, day, hour, minute, second, nano]
                const [year, month, day, hour, minute, second] = timestamp;
                date = new Date(year, month - 1, day, hour, minute, second);
            } else {
                date = new Date(timestamp);
            }

            if (isNaN(date.getTime())) {
                console.warn('無法解析時間戳:', timestamp);
                return '';
            }

            return date.toLocaleTimeString('zh-TW', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            console.error('時間格式化錯誤:', error, timestamp);
            return '';
        }
    }

    /**
     * 滾動到底部
     */
    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    /**
     * 清空聊天記錄
     */
    clearChat() {
        this.messagesContainer.innerHTML = '';
        this.usersList.innerHTML = '';
        this.userCount.textContent = '0';
        this.onlineUsers = [];
        this.messages = [];
    }

    /**
     * 更新連接狀態
     */
    updateConnectionStatus(isConnected) {
        if (!this.connectionStatus) {
            console.error('❌ connectionStatus 元素不存在');
            return;
        }

        if (isConnected) {
            this.connectionStatus.className = 'connection-status connected';
            this.connectionStatus.innerHTML = '<i class="fas fa-circle"></i> 已連接';
        } else {
            this.connectionStatus.className = 'connection-status disconnected';
            this.connectionStatus.innerHTML = '<i class="fas fa-circle"></i> 已斷開';
            this.updateHeartbeatStatus('hidden'); // 隱藏心跳狀態
        }
    }

    /**
     * 顯示心跳狀態（進入聊天室時調用）
     */
    showHeartbeatStatus() {
        console.log('💗 顯示心跳狀態');
        if (!this.heartbeatStatus) {
            console.error('❌ heartbeatStatus 元素不存在');
            return;
        }

        // 設置為半透明狀態，表示尚未啟動心跳
        this.heartbeatStatus.className = 'heartbeat-status excellent';
        this.heartbeatStatus.style.display = 'inline-flex';
        this.heartbeatStatus.style.visibility = 'visible';
        this.heartbeatStatus.style.opacity = '0.4';

        const textElement = this.heartbeatStatus.querySelector('.heartbeat-text');
        if (textElement) textElement.textContent = '待機';

        console.log('💗 心跳狀態已顯示（半透明狀態）');
        console.log('💗 當前元素:', this.heartbeatStatus);
        console.log('💗 當前樣式:', this.heartbeatStatus.style.cssText);
        console.log('💗 當前類別:', this.heartbeatStatus.className);
    }

    /**
     * 顯示 Token 狀態
     */
    showTokenStatus() {
        console.log('🔑 顯示 Token 狀態');
        const tokenStatus = document.getElementById('tokenStatus');
        if (!tokenStatus) {
            console.warn('⚠️ tokenStatus 元素不存在，跳過顯示');
            return;
        }

        // 設置為正常狀態
        tokenStatus.className = 'token-status active normal';
        tokenStatus.style.display = 'inline-flex';
        tokenStatus.style.visibility = 'visible';

        const textElement = tokenStatus.querySelector('.token-text');
        if (textElement) textElement.textContent = 'Token 正常';

        console.log('🔑 Token 狀態已顯示');
    }

    /**
     * 更新心跳狀態
     */
    updateHeartbeatStatus(status, responseTime = 0, message = '') {
        console.log(`🔄 更新心跳狀態: status=${status}, responseTime=${responseTime}, message=${message}`);

        // 檢查元素是否存在
        if (!this.heartbeatStatus) {
            console.error('❌ heartbeatStatus 元素不存在，嘗試重新獲取');
            this.heartbeatStatus = document.getElementById('heartbeatStatus');
            if (!this.heartbeatStatus) {
                console.error('❌ 重新獲取後仍然找不到 heartbeatStatus 元素');
                return;
            }
        }

        console.log('💗 heartbeatStatus 元素存在:', this.heartbeatStatus);
        console.log('💗 當前樣式:', this.heartbeatStatus.style.cssText);
        console.log('💗 當前 className:', this.heartbeatStatus.className);

        const textElement = this.heartbeatStatus.querySelector('.heartbeat-text');
        console.log('📝 找到文字元素:', textElement);

        switch (status) {
            case 'sending':
                this.heartbeatStatus.className = 'heartbeat-status active sending';
                if (textElement) textElement.textContent = '...';
                console.log('📡 設置為發送中狀態');
                break;

            case 'success':
                // 根據響應時間設置訊號強度
                let signalClass = 'excellent'; // 預設為優秀

                if (responseTime <= 250) {
                    signalClass = 'excellent'; // 綠色
                } else if (responseTime <= 300) {
                    signalClass = 'good'; // 黃色
                } else if (responseTime <= 800) {
                    signalClass = 'fair'; // 橘色
                } else {
                    signalClass = 'poor'; // 紅色
                }

                this.heartbeatStatus.className = `heartbeat-status active ${signalClass}`;
                if (textElement) textElement.textContent = `${responseTime}ms`;
                console.log(`✅ 設置為成功狀態: ${signalClass}, ${responseTime}ms`);
                break;

            case 'error':
            case 'timeout':
                this.heartbeatStatus.className = 'heartbeat-status active error';
                if (textElement) textElement.textContent = status === 'timeout' ? '超時' : '錯誤';
                console.log(`❌ 設置為錯誤狀態: ${status}`);
                break;

            case 'hidden':
                this.heartbeatStatus.className = 'heartbeat-status hidden';
                console.log('🫥 隱藏心跳狀態');
                break;

            default:
                // 保持當前狀態
                break;
        }

        // 如果有自定義消息，覆蓋預設文字
        if (message && textElement) {
            textElement.textContent = message;
            console.log(`📝 設置自定義消息: ${message}`);
        }
    }

    /**
     * 更新 Token 狀態顯示
     */
    updateTokenStatus(status, message = '') {
        console.log(`🔑 更新 Token 狀態: status=${status}, message=${message}`);

        // 檢查元素是否存在（將在後續添加到 HTML 中）
        const tokenStatus = document.getElementById('tokenStatus');
        if (!tokenStatus) {
            console.warn('⚠️ tokenStatus 元素不存在，跳過 Token 狀態更新');
            return;
        }

        const textElement = tokenStatus.querySelector('.token-text');

        switch (status) {
            case 'refreshing':
                tokenStatus.className = 'token-status active refreshing';
                if (textElement) textElement.textContent = '刷新中...';
                console.log('🔑 設置為 Token 刷新中狀態');
                break;

            case 'refreshed':
                tokenStatus.className = 'token-status active success';
                if (textElement) textElement.textContent = '已刷新';
                console.log('✅ 設置為 Token 刷新成功狀態');

                // 2秒後恢復正常狀態
                setTimeout(() => {
                    if (tokenStatus) {
                        tokenStatus.className = 'token-status active normal';
                        if (textElement) textElement.textContent = 'Token 正常';
                    }
                }, 2000);
                break;

            case 'error':
                tokenStatus.className = 'token-status active error';
                if (textElement) textElement.textContent = '刷新失敗';
                console.log('❌ 設置為 Token 刷新失敗狀態');
                break;

            case 'expired':
                tokenStatus.className = 'token-status active expired';
                if (textElement) textElement.textContent = '已過期';
                console.log('⏰ 設置為 Token 過期狀態');
                break;

            case 'normal':
                tokenStatus.className = 'token-status active normal';
                if (textElement) textElement.textContent = 'Token 正常';
                console.log('🔑 設置為 Token 正常狀態');
                break;

            case 'hidden':
                tokenStatus.className = 'token-status hidden';
                console.log('🫥 隱藏 Token 狀態');
                break;

            default:
                // 保持當前狀態
                break;
        }

        // 如果有自定義消息，覆蓋預設文字
        if (message && textElement) {
            textElement.textContent = message;
            console.log(`📝 設置自定義 Token 消息: ${message}`);
        }
    }

    /**
     * 顯示登入狀態消息
     */
    showLoginStatus(message, type) {
        if (this.loginStatus) {
            this.loginStatus.textContent = message;
            this.loginStatus.className = `status-message ${type}`;
        } else {
            console.error('❌ loginStatus 元素不存在，無法顯示狀態消息:', message);
        }
    }

    /**
     * 顯示通知
     */
    showNotification(message, type = 'info') {
        if (this.notification) {
            this.notification.textContent = message;
            this.notification.className = `notification ${type}`;
            this.notification.classList.add('show');

            setTimeout(() => {
                if (this.notification) {
                    this.notification.classList.remove('show');
                }
            }, 3000);
        } else {
            console.error('❌ notification 元素不存在，無法顯示通知:', message);
        }
    }

    /**
     * 顯示用戶資訊
     */
    async showUserInfo(userId) {
        if (!userId) {
            console.error('❌ 用戶ID為空，無法顯示用戶資訊');
            return;
        }

        console.log(`👤 顯示用戶資訊: ${userId}`);

        // 顯示彈出框
        if (this.userInfoModal) {
            this.userInfoModal.style.display = 'flex';
        }

        // 顯示載入中狀態
        if (this.userInfoContent) {
            this.userInfoContent.innerHTML = `
                <div class="loading-message">
                    <i class="fas fa-spinner fa-spin"></i> 載入中...
                </div>
            `;
        }

        try {
            // 獲取用戶資訊
            const response = await this.client.getUserInfo(userId);
            console.log('📋 用戶資訊響應:', response);

            // 解析響應
            let userData = null;
            if (response.buffer) {
                try {
                    userData = JSON.parse(response.buffer);
                } catch (e) {
                    console.error('❌ 解析用戶資訊響應失敗:', e);
                    userData = response.buffer;
                }
            }

            if (userData && userData.code === 200 && userData.user) {
                this.renderUserInfo(userData.user);
                this.showNotification('用戶資訊載入成功', 'success');
            } else if (userData && userData.code === 404) {
                this.renderUserInfoError('用戶不存在');
                this.showNotification('用戶不存在', 'warning');
            } else {
                this.renderUserInfoError('獲取用戶資訊失敗');
                this.showNotification('獲取用戶資訊失敗', 'error');
            }
        } catch (error) {
            console.error('❌ 獲取用戶資訊失敗:', error);
            this.renderUserInfoError('網路錯誤，請稍後再試');
            this.showNotification('獲取用戶資訊失敗: ' + error.message, 'error');
        }
    }

    /**
     * 渲染用戶資訊
     */
    renderUserInfo(user) {
        if (!this.userInfoContent) {
            console.error('❌ userInfoContent 元素不存在');
            return;
        }

        const html = `
            <div class="user-info-avatar">
                <i class="fas fa-user-circle"></i>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">用戶ID:</div>
                <div class="user-info-value">${user.userId || 'N/A'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">用戶名:</div>
                <div class="user-info-value">${user.userName || user.userId || 'N/A'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">狀態:</div>
                <div class="user-info-value">
                    <span style="color: ${user.online ? '#22c55e' : '#6b7280'};">
                        <i class="fas fa-circle" style="font-size: 8px; margin-right: 5px;"></i>
                        ${user.online ? '在線' : '離線'}
                    </span>
                </div>
            </div>
            ${user.lastActiveTime ? `
            <div class="user-info-item">
                <div class="user-info-label">最後活動:</div>
                <div class="user-info-value">${new Date(user.lastActiveTime).toLocaleString()}</div>
            </div>
            ` : ''}
            ${user.joinTime ? `
            <div class="user-info-item">
                <div class="user-info-label">加入時間:</div>
                <div class="user-info-value">${new Date(user.joinTime).toLocaleString()}</div>
            </div>
            ` : ''}
        `;

        this.userInfoContent.innerHTML = html;
    }

    /**
     * 渲染用戶資訊錯誤
     */
    renderUserInfoError(message) {
        if (!this.userInfoContent) {
            console.error('❌ userInfoContent 元素不存在');
            return;
        }

        this.userInfoContent.innerHTML = `
            <div class="error-message">
                <i class="fas fa-exclamation-triangle"></i>
                <p>${message}</p>
            </div>
        `;
    }

    /**
     * 關閉用戶資訊彈出框
     */
    closeUserInfo() {
        if (this.userInfoModal) {
            this.userInfoModal.style.display = 'none';
        }
    }

    /**
     * 顯示載入中
     */
    showLoading() {
        if (this.loadingOverlay) {
            this.loadingOverlay.style.display = 'flex';
        } else {
            console.error('❌ loadingOverlay 元素不存在');
        }
    }

    /**
     * 隱藏載入中
     */
    hideLoading() {
        if (this.loadingOverlay) {
            this.loadingOverlay.style.display = 'none';
        } else {
            console.error('❌ loadingOverlay 元素不存在');
        }
    }

    /**
     * 清除調試日誌 (防呆方法，避免調試相關錯誤)
     */
    clearDebugLog() {
        // 如果有調試元素，清除其內容
        const debugElement = document.getElementById('debugLog');
        if (debugElement) {
            debugElement.textContent = '';
        }
        // 不報錯，靜默處理
    }

    /**
     * 初始化主題設置
     */
    initializeTheme() {
        console.log('🎨 初始化主題設置...');

        // 從 localStorage 讀取保存的主題設置，默認為淺色模式
        const savedTheme = localStorage.getItem('chatapp-theme') || 'light';
        console.log('🎨 讀取保存的主題:', savedTheme);

        this.setTheme(savedTheme);
        this.updateThemeButtonIcon(savedTheme);
    }

    /**
     * 切換主題
     */
    toggleTheme() {
        console.log('🎨 切換主題...');

        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';

        console.log('🎨 當前主題:', currentTheme, '→ 新主題:', newTheme);

        this.setTheme(newTheme);
        this.updateThemeButtonIcon(newTheme);

        // 保存主題設置到 localStorage
        localStorage.setItem('chatapp-theme', newTheme);

        // 顯示通知
        this.showNotification(`已切換到${newTheme === 'dark' ? '深色' : '淺色'}模式`, 'info');
    }

    /**
     * 設置主題
     */
    setTheme(theme) {
        console.log('🎨 設置主題:', theme);
        document.documentElement.setAttribute('data-theme', theme);
    }

    /**
     * 更新主題切換按鈕圖標
     */
    updateThemeButtonIcon(theme) {
        // 更新聊天頁面的主題按鈕
        if (this.themeToggleBtn) {
            const chatIcon = this.themeToggleBtn.querySelector('i');
            if (chatIcon) {
                // 移除舊的圖標類
                chatIcon.classList.remove('fa-moon', 'fa-sun');

                // 根據當前主題設置圖標
                if (theme === 'dark') {
                    chatIcon.classList.add('fa-sun'); // 深色模式下顯示太陽圖標
                    this.themeToggleBtn.title = '切換到淺色模式';
                } else {
                    chatIcon.classList.add('fa-moon'); // 淺色模式下顯示月亮圖標
                    this.themeToggleBtn.title = '切換到深色模式';
                }
            }
        }

        // 更新登入頁面的主題按鈕
        if (this.loginThemeToggleBtn) {
            const loginIcon = this.loginThemeToggleBtn.querySelector('i');
            if (loginIcon) {
                // 移除舊的圖標類
                loginIcon.classList.remove('fa-moon', 'fa-sun');

                // 根據當前主題設置圖標
                if (theme === 'dark') {
                    loginIcon.classList.add('fa-sun'); // 深色模式下顯示太陽圖標
                    this.loginThemeToggleBtn.title = '切換到淺色模式';
                } else {
                    loginIcon.classList.add('fa-moon'); // 淺色模式下顯示月亮圖標
                    this.loginThemeToggleBtn.title = '切換到深色模式';
                }
            }
        }

        console.log('🎨 主題按鈕圖標已更新:', theme === 'dark' ? 'sun' : 'moon');
    }

}

// 初始化應用程序
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 DOM 載入完成，初始化聊天應用程序...');
    try {
        window.chatApp = new ChatApp();
        console.log('✅ 聊天應用程序初始化成功');
    } catch (error) {
        console.error('❌ 聊天應用程序初始化失敗:', error);
    }
});
