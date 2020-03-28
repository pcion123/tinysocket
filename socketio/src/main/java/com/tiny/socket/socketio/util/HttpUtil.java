package com.tiny.socket.socketio.util;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tiny.socket.socketio.util.http.HttpBaseResponse;
import com.tiny.socket.socketio.util.http.HttpGetTask;
import com.tiny.socket.socketio.util.http.HttpParamPostTask;
import com.tiny.socket.socketio.util.http.HttpStringPostTask;

public class HttpUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private final static PoolingHttpClientConnectionManager pools =
            new PoolingHttpClientConnectionManager();

    private final static RequestConfig config = RequestConfig.custom().setConnectTimeout(10000)
            .setSocketTimeout(10000).setConnectionRequestTimeout(10000).build();

    public final static HttpClientBuilder builder = HttpClients.custom().setConnectionManager(pools)
            .setConnectionManagerShared(true).setDefaultRequestConfig(config);

    static {
        pools.setMaxTotal(200);
        pools.setDefaultMaxPerRoute(20);
    }

    // 普通get
    public static HttpBaseResponse doGet(String url) {
        HttpGetTask task = new HttpGetTask(url, null, 60000L, null, null, null);
        return task.work();
    }

    // 普通get
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doGet(String url, Map headers) {
        HttpGetTask task = new HttpGetTask(url, headers, 60000L, null, null, null);
        return task.work();
    }

    // 逾時get(無pools)
    public static HttpBaseResponse doGet(String url, long timeout) {
        HttpGetTask task = new HttpGetTask(url, null, timeout, null, null, null);
        return task.work();
    }

    // 逾時get(無pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doGet(String url, Map headers, long timeout) {
        HttpGetTask task = new HttpGetTask(url, headers, timeout, null, null, null);
        return task.work();
    }

    // 逾時get(有pools)
    public static HttpBaseResponse doGet(String url, long timeout, ExecutorService pools) {
        return doGet(url, null, timeout, pools);
    }

    // 逾時get(有pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doGet(String url, Map headers, long timeout,
            ExecutorService pools) {
        HttpGetTask task = new HttpGetTask(url, headers, timeout, null, null, pools);
        return task.work();
    }

    // 逾時get(無pools,callback)
    public static void doGet(String url, long timeout, Consumer<HttpBaseResponse> callback,
            Consumer<HttpBaseResponse> error) {
        doGet(url, null, timeout, callback, error, null);
    }

    // 逾時get(無pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doGet(String url, Map headers, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error) {
        doGet(url, headers, timeout, callback, error, null);
    }

    // 逾時get(有pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doGet(String url, Map headers, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error,
            ExecutorService pools) {
        try {
            if (pools != null) {
                pools.execute(new HttpGetTask(url, headers, timeout, callback, error, pools));
            } else {
                Thread t =
                        new Thread(new HttpGetTask(url, headers, timeout, callback, error, pools));
                t.start();
            }
        } catch (RejectedExecutionException e1) {
            if (url != null) {
                logger.error("reject error message={} url={}", e1.getMessage(), url);
            } else {
                logger.error("reject error message={}", e1.getMessage());
            }
            throw e1;
        } catch (Exception e2) {
            if (url != null) {
                logger.error("unknown error message={} url={}", e2.getMessage(), url);
            } else {
                logger.error("unknown error message={}", e2.getMessage());
            }
            throw e2;
        }
    }

    // 普通post
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map params) {
        HttpParamPostTask task = new HttpParamPostTask(url, null, params, 60000L, null, null, null);
        return task.work();
    }

    // 普通post
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, Map params) {
        HttpParamPostTask task =
                new HttpParamPostTask(url, headers, params, 60000L, null, null, null);
        return task.work();
    }

    // 逾時post(無pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map params, long timeout) {
        HttpParamPostTask task =
                new HttpParamPostTask(url, null, params, timeout, null, null, null);
        return task.work();
    }

    // 逾時post(無pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, Map params, long timeout) {
        HttpParamPostTask task =
                new HttpParamPostTask(url, headers, params, timeout, null, null, null);
        return task.work();
    }

    // 逾時post(有pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map params, long timeout,
            ExecutorService pools) {
        HttpParamPostTask task =
                new HttpParamPostTask(url, null, params, timeout, null, null, pools);
        return task.work();
    }

    // 逾時post(有pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, Map params, long timeout,
            ExecutorService pools) {
        HttpParamPostTask task =
                new HttpParamPostTask(url, headers, params, timeout, null, null, pools);
        return task.work();
    }

    // 逾時post(無pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doPost(String url, Map params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error) {
        doPost(url, null, params, timeout, callback, error, null);
    }

    // 逾時post(無pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doPost(String url, Map headers, Map params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error) {
        doPost(url, headers, params, timeout, callback, error, null);
    }

    // 逾時post(有pools且callback)
    @SuppressWarnings("rawtypes")
    public static void doPost(String url, Map headers, Map params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error,
            ExecutorService pools) {
        try {
            if (pools != null) {
                pools.execute(new HttpParamPostTask(url, headers, params, timeout, callback, error,
                        pools));
            } else {
                Thread t = new Thread(new HttpParamPostTask(url, headers, params, timeout, callback,
                        error, pools));
                t.start();
            }
        } catch (RejectedExecutionException e1) {
            if (url != null) {
                logger.error("reject error message={} url={} params={}", e1.getMessage(), url,
                        JsonUtil.obj2JsonStr(params));
            } else {
                logger.error("reject error message={}", e1.getMessage());
            }
            throw e1;
        } catch (Exception e2) {
            if (url != null) {
                logger.error("unknown error message={} url={} params={}", e2.getMessage(), url,
                        JsonUtil.obj2JsonStr(params));
            } else {
                logger.error("unknown error message={}", e2.getMessage());
            }
            throw e2;
        }
    }

    // 普通post(json)
    public static HttpBaseResponse doPost(String url, String params) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, null, params, 60000L, null, null, null);
        return task.work();
    }

    // 普通post(json)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, String params) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, headers, params, 60000L, null, null, null);
        return task.work();
    }

    // 逾時post(json,無pools)
    public static HttpBaseResponse doPost(String url, String params, long timeout) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, null, params, timeout, null, null, null);
        return task.work();
    }

    // 逾時post(json,無pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, String params, long timeout) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, headers, params, timeout, null, null, null);
        return task.work();
    }

    // 逾時post(json,有pools)
    public static HttpBaseResponse doPost(String url, String params, long timeout,
            ExecutorService pools) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, null, params, timeout, null, null, pools);
        return task.work();
    }

    // 逾時post(json,有pools)
    @SuppressWarnings("rawtypes")
    public static HttpBaseResponse doPost(String url, Map headers, String params, long timeout,
            ExecutorService pools) {
        HttpStringPostTask task =
                new HttpStringPostTask(url, headers, params, timeout, null, null, pools);
        return task.work();
    }

    // 逾時post(json,無pools,callback)
    public static void doPost(String url, String params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error) {
        doPost(url, null, params, timeout, callback, error, null);
    }

    // 逾時post(json,無pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doPost(String url, Map headers, String params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error) {
        doPost(url, headers, params, timeout, callback, error, null);
    }

    // 逾時post(json,有pools,callback)
    @SuppressWarnings("rawtypes")
    public static void doPost(String url, Map headers, String params, long timeout,
            Consumer<HttpBaseResponse> callback, Consumer<HttpBaseResponse> error,
            ExecutorService pools) {
        try {
            if (pools != null) {
                pools.execute(new HttpStringPostTask(url, headers, params, timeout, callback, error,
                        pools));
            } else {
                Thread t = new Thread(new HttpStringPostTask(url, headers, params, timeout,
                        callback, error, pools));
                t.start();
            }
        } catch (RejectedExecutionException e1) {
            if (url != null) {
                logger.error("reject error message={} url={} params={}", e1.getMessage(), url,
                        params);
            } else {
                logger.error("reject error message={}", e1.getMessage());
            }
            throw e1;
        } catch (Exception e2) {
            if (url != null) {
                logger.error("unknown error message={} url={} params={}", e2.getMessage(), url,
                        params);
            } else {
                logger.error("unknown error message={}", e2.getMessage());
            }
            throw e2;
        }
    }
}
