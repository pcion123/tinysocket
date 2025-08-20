package com.vscodelife.socketio.util.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public final class HttpUtil {

    // 請求池配置
    private static final int MAX_CONCURRENT_REQUESTS = 10; // 最大併發請求數
    private static final long MIN_REQUEST_INTERVAL = 100; // 最小請求間隔（毫秒）

    // 請求控制器
    private static final Semaphore requestSemaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
    private static final AtomicLong lastRequestTime = new AtomicLong(0);
    private static final Object requestTimeLock = new Object();

    // 私有建構函數，防止實例化
    private HttpUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 請求控制 - 獲取請求許可
     * 
     * @param timeout 超時時間（毫秒）
     * @return 是否成功獲取許可
     */
    private static boolean acquireRequestPermit(long timeout) {
        try {
            // 嘗試獲取信號量許可
            boolean acquired = requestSemaphore.tryAcquire(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!acquired) {
                return false;
            }

            // 控制請求間隔
            synchronized (requestTimeLock) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime.get();

                if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
                    long sleepTime = MIN_REQUEST_INTERVAL - timeSinceLastRequest;
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        requestSemaphore.release();
                        return false;
                    }
                }

                lastRequestTime.set(System.currentTimeMillis());
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 釋放請求許可
     */
    private static void releaseRequestPermit() {
        requestSemaphore.release();
    }

    /**
     * 獲取當前可用的請求許可數量
     * 
     * @return 可用許可數量
     */
    public static int getAvailablePermits() {
        return requestSemaphore.availablePermits();
    }

    /**
     * 獲取正在等待的請求數量
     * 
     * @return 等待中的請求數量
     */
    public static int getQueueLength() {
        return requestSemaphore.getQueueLength();
    }

    /**
     * 執行 HTTP GET 請求（帶請求池控制）
     * 
     * @param url     請求的 URL
     * @param timeout 超時時間（毫秒）
     * @return HttpResponse 回應物件
     */
    public static HttpResponse get(String url, long timeout) {
        if (url == null || url.trim().isEmpty()) {
            return new HttpResponse(400, "URL cannot be null or empty");
        }

        // 請求池控制 - 獲取許可
        if (!acquireRequestPermit(timeout / 2)) { // 使用一半的超時時間等待許可
            return new HttpResponse(429, "Too many requests - request pool exhausted");
        }

        HttpURLConnection connection = null;
        try {
            // 建立連線
            URL urlObj = URI.create(url).toURL();
            connection = (HttpURLConnection) urlObj.openConnection();

            // 設定請求參數
            connection.setRequestMethod("GET");
            connection.setConnectTimeout((int) timeout);
            connection.setReadTimeout((int) timeout);
            connection.setRequestProperty("User-Agent", "HttpUtil/1.0");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());

            // 執行請求
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            // 讀取回應內容
            String responseBody = readResponse(connection, responseCode);

            return new HttpResponse(responseCode, responseMessage, responseBody);

        } catch (SocketTimeoutException e) {
            return new HttpResponse(408, "Request timeout: " + e.getMessage());
        } catch (IOException e) {
            return new HttpResponse(500, "IO Exception: " + e.getMessage());
        } catch (Exception e) {
            return new HttpResponse(500, "Unexpected error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            // 釋放請求許可
            releaseRequestPermit();
        }
    }

    /**
     * 讀取 HTTP 回應內容
     * 
     * @param connection   HTTP 連線
     * @param responseCode 回應狀態碼
     * @return 回應內容字串
     * @throws IOException IO 異常
     */
    private static String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        BufferedReader reader = null;
        try {
            // 根據狀態碼選擇輸入流
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(
                        connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            // 讀取回應內容
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line).append("\n");
            }

            // 移除最後的換行符
            if (responseBody.length() > 0) {
                responseBody.setLength(responseBody.length() - 1);
            }

            return responseBody.toString();

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 忽略關閉異常
                }
            }
        }
    }

    /**
     * 執行 HTTP POST 請求（帶請求池控制）
     * 
     * @param url     請求的 URL
     * @param params  POST 參數
     * @param timeout 超時時間（毫秒）
     * @return HttpResponse 回應物件
     */
    public static HttpResponse post(String url, Map<String, Object> params, long timeout) {
        if (url == null || url.trim().isEmpty()) {
            return new HttpResponse(400, "URL cannot be null or empty");
        }

        // 請求池控制 - 獲取許可
        if (!acquireRequestPermit(timeout / 2)) { // 使用一半的超時時間等待許可
            return new HttpResponse(429, "Too many requests - request pool exhausted");
        }

        HttpURLConnection connection = null;
        try {
            // 建立連線
            URL urlObj = URI.create(url).toURL();
            connection = (HttpURLConnection) urlObj.openConnection();

            // 設定請求參數
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) timeout);
            connection.setReadTimeout((int) timeout);
            connection.setDoOutput(true);
            connection.setRequestProperty("User-Agent", "HttpUtil/1.0");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());

            // 準備 POST 資料
            String postData = buildPostData(params);

            // 寫入 POST 資料
            if (postData != null && !postData.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
            }

            // 執行請求
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            // 讀取回應內容
            String responseBody = readResponse(connection, responseCode);

            return new HttpResponse(responseCode, responseMessage, responseBody);

        } catch (SocketTimeoutException e) {
            return new HttpResponse(408, "Request timeout: " + e.getMessage());
        } catch (IOException e) {
            return new HttpResponse(500, "IO Exception: " + e.getMessage());
        } catch (Exception e) {
            return new HttpResponse(500, "Unexpected error: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            // 釋放請求許可
            releaseRequestPermit();
        }
    }

    /**
     * 建立 POST 資料字串
     * 
     * @param params 參數 Map
     * @return URL 編碼的參數字串
     */
    private static String buildPostData(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                result.append("&");
            }

            String key = entry.getKey();
            Object value = entry.getValue();

            if (key != null && value != null) {
                try {
                    String encodedKey = java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name());
                    String encodedValue = java.net.URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name());
                    result.append(encodedKey).append("=").append(encodedValue);
                } catch (Exception e) {
                    // 如果編碼失敗，使用原始值
                    result.append(key).append("=").append(value.toString());
                }
            }

            first = false;
        }

        return result.toString();
    }

    /**
     * 測試 main 方法
     * 
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        System.out.println("=== HttpUtil 測試 ===");

        // // 測試 GET 方法
        // testGetMethod();

        // System.out.println("\n" + "=".repeat(50) + "\n");

        // 測試 POST 方法
        testPostMethod();

        // System.out.println("\n" + "=".repeat(50) + "\n");

        // // 測試請求池機制
        // testRequestPool();
    }

    /**
     * 測試 GET 方法
     */
    private static void testGetMethod() {
        System.out.println("=== GET 方法測試 ===");

        String testUrl = "https://www.google.com";
        long timeout = 10000; // 10 秒超時

        System.out.println("測試 URL: " + testUrl);
        System.out.println("超時設定: " + timeout + "ms");
        System.out.println();

        try {
            System.out.println("執行 HTTP GET 請求...");
            HttpResponse response = HttpUtil.get(testUrl, timeout);

            System.out.println("=== GET 回應結果 ===");
            System.out.println("狀態碼: " + response.getCode());
            System.out.println("狀態訊息: " + response.getMessage());
            System.out.println();

            // 顯示回應內容的部分資訊
            String responseBody = response.getResponse();
            if (responseBody != null && !responseBody.isEmpty()) {
                System.out.println("回應內容長度: " + responseBody.length() + " 字元");

                // 顯示前 200 個字元
                int previewLength = Math.min(200, responseBody.length());
                String preview = responseBody.substring(0, previewLength);
                System.out.println("回應內容預覽:");
                System.out.println(preview);
                if (responseBody.length() > previewLength) {
                    System.out.println("...(內容過長，僅顯示前 " + previewLength + " 字元)");
                }
            } else {
                System.out.println("回應內容: 無");
            }

            System.out.println("\n=== GET 測試完成 ===");

        } catch (Exception e) {
            System.err.println("GET 測試過程中發生異常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 測試 POST 方法
     */
    private static void testPostMethod() {
        System.out.println("=== POST 方法測試 ===");

        // 使用 httpbin.org 測試 POST
        String testUrl = "https://httpbin.org/anything";
        long timeout = 10000;

        // 準備測試參數
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("name", "HttpUtil測試");
        params.put("version", "1.0");
        params.put("timestamp", System.currentTimeMillis());
        params.put("test", "POST方法測試");

        System.out.println("測試 URL: " + testUrl);
        System.out.println("超時設定: " + timeout + "ms");
        System.out.println("POST 參數: " + params);
        System.out.println();

        try {
            System.out.println("執行 HTTP POST 請求...");
            HttpResponse response = HttpUtil.post(testUrl, params, timeout);

            System.out.println("=== POST 回應結果 ===");
            System.out.println("狀態碼: " + response.getCode());
            System.out.println("狀態訊息: " + response.getMessage());
            System.out.println();

            // 顯示回應內容
            String responseBody = response.getResponse();
            if (responseBody != null && !responseBody.isEmpty()) {
                System.out.println("回應內容長度: " + responseBody.length() + " 字元");
                System.out.println("回應內容:");
                System.out.println(responseBody);
            } else {
                System.out.println("回應內容: 無");
            }

            System.out.println("\n=== POST 測試完成 ===");

        } catch (Exception e) {
            System.err.println("POST 測試過程中發生異常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 測試請求池機制
     */
    private static void testRequestPool() {
        System.out.println("=== 請求池機制測試 ===");
        System.out.println("最大併發請求數: " + MAX_CONCURRENT_REQUESTS);
        System.out.println("最小請求間隔: " + MIN_REQUEST_INTERVAL + "ms");
        System.out.println();

        // 顯示當前請求池狀態
        System.out.println("當前可用許可: " + getAvailablePermits());
        System.out.println("等待中的請求: " + getQueueLength());
        System.out.println();

        // 模擬多個併發請求
        System.out.println("模擬 15 個併發請求（超過池容量）...");

        for (int i = 1; i <= 15; i++) {
            final int requestId = i;
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                System.out.println("請求 " + requestId + " 開始");

                HttpResponse response = HttpUtil.get("https://httpbin.org/delay/1", 30000);

                long endTime = System.currentTimeMillis();
                System.out.printf("請求 %d 完成 - 狀態碼: %d, 耗時: %dms%n",
                        requestId, response.getCode(), (endTime - startTime));
            }).start();

            // 讓請求錯開一點時間
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 等待一段時間讓請求完成
        try {
            Thread.sleep(20000); // 等待 20 秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 顯示最終狀態
        System.out.println("\n=== 請求池測試完成 ===");
        System.out.println("最終可用許可: " + getAvailablePermits());
        System.out.println("等待中的請求: " + getQueueLength());
    }
}