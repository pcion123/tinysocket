package com.tiny.socket.socketio.util.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tiny.socket.socketio.util.HttpUtil;

public class HttpStringPostTask implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(HttpStringPostTask.class);

    private final static String CHARSET = "UTF-8";

    private boolean running = true;
    private long timeout;
    private Task task;
    private ExecutorService pools;
    private Consumer<HttpBaseResponse> error;

    @SuppressWarnings("rawtypes")
    public HttpStringPostTask(String url, Map headers, String params, long timeout,
            Consumer<HttpBaseResponse> finish, Consumer<HttpBaseResponse> error,
            ExecutorService pools) {
        this.timeout = timeout;
        this.task = new Task(url, headers, params, timeout, finish);
        this.pools = pools;
        this.error = error;
    }

    public boolean running() {
        return running;
    }

    public void shutdown() {
        try {
            if (task != null)
                task.shutdown();

            running = false;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("static-access")
    public HttpBaseResponse work() {
        HttpBaseResponse response = null;
        try {
            if (pools != null) {
                pools.execute(task);
            } else {
                Thread t = new Thread(task);
                t.start();
            }
            long timestamp = System.currentTimeMillis();
            while (task.running()) {
                long now = System.currentTimeMillis();
                if (now - timestamp > timeout)
                    task.shutdown(true);
                Thread.currentThread().sleep(100);
            }
        } catch (RejectedExecutionException e1) {
            response = new HttpBaseResponse(11, "task pool is busy.");
        } catch (Exception e2) {
            response = new HttpBaseResponse(12, "unknown error=" + e2.getMessage());
        } finally {
            response = response == null ? task.getResult() : response;
        }
        return response;
    }

    @SuppressWarnings("static-access")
    @Override
    public void run() {
        running = true;
        try {
            if (pools != null) {
                pools.execute(task);
            } else {
                Thread t = new Thread(task);
                t.start();
            }
            long timestamp = System.currentTimeMillis();
            while (task.running()) {
                long now = System.currentTimeMillis();
                if (now - timestamp > timeout)
                    task.shutdown(true);
                Thread.currentThread().sleep(100);
            }
        } catch (RejectedExecutionException e1) {
            if (error != null)
                error.accept(new HttpBaseResponse(11, "task pool is busy."));
        } catch (Exception e2) {
            if (error != null)
                error.accept(new HttpBaseResponse(12, "unknown error=" + e2.getMessage()));
        }
        running = false;
    }

    private class Task implements Runnable {
        private boolean running = true;
        private int status = 999;
        private String message = "unknown error";
        private String result = null;
        private CloseableHttpClient client;
        private CloseableHttpResponse response;
        private String url;
        @SuppressWarnings("rawtypes")
        private Map headers;
        private String params;
        private long timeout;
        private Consumer<HttpBaseResponse> finish;

        @SuppressWarnings("rawtypes")
        public Task(String url, Map headers, String params, long timeout,
                Consumer<HttpBaseResponse> finish) {
            this.url = url;
            this.headers = headers;
            this.params = params;
            this.timeout = timeout;
            this.finish = finish;
        }

        @SuppressWarnings("unused")
        public String getUrl() {
            return url;
        }

        @SuppressWarnings({"unused", "rawtypes"})
        public Map getHeaders() {
            return headers;
        }

        @SuppressWarnings("unused")
        public String getParams() {
            return params;
        }

        public HttpBaseResponse getResult() {
            return new HttpBaseResponse(status, message, result);
        }

        public boolean running() {
            return running;
        }

        public void shutdown() {
            shutdown(false);
        }

        public void shutdown(boolean timeup) {
            try {
                if (response != null)
                    response.close();

                if (timeup) {
                    status = 995;
                    message = "connection timeup";
                    result = null;
                } else {
                    status = 994;
                    message = "connection shutdown";
                    result = null;
                }

                running = false;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void run() {
            running = true;

            status = 999;
            message = "unknown error";
            result = null;
            BufferedReader in = null;
            try {
                client = HttpUtil.builder.build();
                HttpPost request = new HttpPost(url);
                request.setHeader("Accept", "application/json");
                request.setHeader("Content-Type", "application/json");
                if (timeout != 0) {
                    RequestConfig config = RequestConfig.custom().setConnectTimeout((int) timeout)
                            .setConnectionRequestTimeout((int) timeout)
                            .setSocketTimeout((int) timeout).build();
                    request.setConfig(config);
                }
                if (headers != null) {
                    for (Iterator iter = headers.keySet().iterator(); iter.hasNext();) {
                        String key = (String) iter.next();
                        String value = String.valueOf(headers.get(key));
                        request.setHeader(key, value);
                    }
                }
                if (params != null) {
                    request.setEntity(new StringEntity(params, CHARSET));
                }
                response = client.execute(request);
                status = response.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    in = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent(), CHARSET));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";
                    String nl = System.getProperty("line.separator");
                    while ((line = in.readLine()) != null) {
                        sb.append(line + nl);
                    }
                    in.close();
                    message = "success";
                    result = sb.toString();
                } else {
                    in = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent(), CHARSET));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";
                    String nl = System.getProperty("line.separator");
                    while ((line = in.readLine()) != null) {
                        sb.append(line + nl);
                    }
                    in.close();
                    message = "fail";
                    result = sb.toString();
                }
            } catch (SocketTimeoutException e1) {
                status = 996;
                message = e1.getMessage();
                result = null;
            } catch (HttpHostConnectException e2) {
                status = 997;
                message = e2.getMessage();
                result = null;
            } catch (IllegalStateException e3) {
                status = 998;
                message = e3.getMessage();
                result = null;
            } catch (Exception e4) {
                status = 999;
                message = "unknown error=" + e4.getMessage();
                result = null;
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                try {
                    if (response != null)
                        response.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (finish != null)
                finish.accept(new HttpBaseResponse(status, message, result));

            running = false;
        }
    }
}
