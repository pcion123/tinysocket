package com.tiny.socket.socketio.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tiny.socket.socketio.util.http.HttpGetTask;
import com.tiny.socket.socketio.util.http.HttpParamPostTask;
import com.tiny.socket.socketio.util.http.HttpStringPostTask;

public class HttpUtil
{
	private static Logger logger = LoggerFactory.getLogger(HttpUtil.class);
	
	private final static String CHARSET = "UTF-8";
	
	private final static PoolingHttpClientConnectionManager pools = new PoolingHttpClientConnectionManager();
	
	private final static RequestConfig config = RequestConfig.custom()
			.setConnectTimeout(10000)
			.setSocketTimeout(10000)
			.setConnectionRequestTimeout(10000)
			.build();
	
	public final static HttpClientBuilder builder = HttpClients.custom()
			.setConnectionManager(pools)
			.setConnectionManagerShared(true)
			.setDefaultRequestConfig(config);
	
	static
	{
		pools.setMaxTotal(200);
		pools.setDefaultMaxPerRoute(20);
	}
	
	// 普通get
	public static String doGet(String url)
	{
		String result = null;
        try
        {
        	HttpClient client = builder.build();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK)
            {
            	result = EntityUtils.toString(response.getEntity());
            }
            else
            {
            	logger.info("http get result has error -> {}", status);
            }
        }
        catch (IOException e)
        {
        	logger.error(e.getMessage(), e);
        }
        return result;
	}
	
	// 逾時get(無pools)
	public static String doGet(String url, long timeout)
	{
		HttpGetTask task = new HttpGetTask(url, timeout, null, null, null);
		return task.work();
	}
	
	// 逾時get(有pools)
	public static String doGet(String url, long timeout, ExecutorService pools)
	{
		HttpGetTask task = new HttpGetTask(url, timeout, null, null, pools);
		return task.work();
	}
	
	// 逾時get(無pools且callback)
	public static void doGet(String url, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error)
	{
		doGet(url, timeout, callback, error, null);
	}
	
	// 逾時get(有pools且callback)
	public static void doGet(String url, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error, ExecutorService pools)
	{
		try
		{
			if (pools != null)
			{
				pools.execute(new HttpGetTask(url, timeout, callback, error, pools));
			}
			else
			{
				Thread t = new Thread(new HttpGetTask(url, timeout, callback, error, pools));
				t.start();
			}
		}
		catch (RejectedExecutionException e1)
		{
			if (url != null)
			{
				logger.error("reject error message={} url={}", e1.getMessage(), url);
			}
			else
			{
				logger.error("reject error message={}", e1.getMessage());
			}
			throw e1;
		}
		catch (Exception e2)
		{
			if (url != null)
			{
				logger.error("unknown error message={} url={}", e2.getMessage(), url);
			}
			else
			{
				logger.error("unknown error message={}", e2.getMessage());
			}
			throw e2;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static String doPost(String url, Map params)
	{
		BufferedReader in = null;
		String result = null;
        try
        {
        	HttpClient client = builder.build();
            HttpPost request = new HttpPost(url);
            if (params != null)
            {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                for (Iterator iter = params.keySet().iterator(); iter.hasNext();)
                {
        			String key = (String)iter.next();
        			String value = String.valueOf(params.get(key));
        			nvps.add(new BasicNameValuePair(key, value));
        			//System.out.println(String.format("%s-%s", key, value));
        		}
                request.setEntity(new UrlEncodedFormEntity(nvps, CHARSET));
            }
            //request.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK)
            {
            	in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), CHARSET));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String nl = System.getProperty("line.separator");
                while ((line = in.readLine()) != null)
                {
                    sb.append(line + nl);
                }
                in.close();
                result = sb.toString();
            }
            else
            {
            	logger.info("http post result has error -> {}", status);
            }
        }
        catch (Exception e)
        {
        	logger.error(e.getMessage(), e);
        }
        finally
        {
        	try
        	{
            	if (in != null)
            		in.close();
        	}
        	catch (Exception e)
        	{
        		logger.error(e.getMessage(), e);
        	}
        }
        return result;
	}
	
	@SuppressWarnings("rawtypes")
	public static String doPost(String url, Map params, long timeout)
	{
		HttpParamPostTask task = new HttpParamPostTask(url, params, timeout, null, null, null);
		return task.work();
	}
	
	@SuppressWarnings("rawtypes")
	public static String doPost(String url, Map params, long timeout, ExecutorService pools)
	{
		HttpParamPostTask task = new HttpParamPostTask(url, params, timeout, null, null, pools);
		return task.work();
	}
	
	@SuppressWarnings("rawtypes")
	public static void doPost(String url, Map params, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error)
	{
		doPost(url, params, timeout, callback, error, null);
	}
	
	@SuppressWarnings("rawtypes")
	public static void doPost(String url, Map params, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error, ExecutorService pools)
	{
		try
		{
			if (pools != null)
			{
				pools.execute(new HttpParamPostTask(url, params, timeout, callback, error, pools));
			}
			else
			{
				Thread t = new Thread(new HttpParamPostTask(url, params, timeout, callback, error, pools));
				t.start();
			}
		}
		catch (RejectedExecutionException e1)
		{
			if (url != null)
			{
				logger.error("reject error message={} url={} params={}", e1.getMessage(), url, JsonUtil.obj2JsonStr(params));
			}
			else
			{
				logger.error("reject error message={}", e1.getMessage());
			}
			throw e1;
		}
		catch (Exception e2)
		{
			if (url != null)
			{
				logger.error("unknown error message={} url={} params={}", e2.getMessage(), url, JsonUtil.obj2JsonStr(params));
			}
			else
			{
				logger.error("unknown error message={}", e2.getMessage());
			}
			throw e2;
		}
	}
	
	public static String doPost(String url, String params)
	{
		BufferedReader in = null;
		String result = null;
		try
		{
			HttpClient client = builder.build();
        	HttpPost request = new HttpPost(url);
        	request.setHeader("Accept", "application/json"); 
        	request.setHeader("Content-Type", "application/json");
        	if (params != null)
        	{
        		request.setEntity(new StringEntity(params, CHARSET));
        	}
        	HttpResponse response = client.execute(request);
        	int status = response.getStatusLine().getStatusCode();
        	if (status == HttpStatus.SC_OK)
        	{
            	in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), CHARSET));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String nl = System.getProperty("line.separator");
                while ((line = in.readLine()) != null)
                {
                    sb.append(line + nl);
                }
                in.close();
                result = sb.toString();
        	}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
        finally
        {
        	try
        	{
            	if (in != null)
            		in.close();
        	}
        	catch (Exception e)
        	{
        		logger.error(e.getMessage(), e);
        	}
        }
        return result;
	}
	
	public static String doPost(String url, String params, long timeout)
	{
		HttpStringPostTask task = new HttpStringPostTask(url, params, timeout, null, null, null);
		return task.work();
	}
	
	public static String doPost(String url, String params, long timeout, ExecutorService pools)
	{
		HttpStringPostTask task = new HttpStringPostTask(url, params, timeout, null, null, pools);
		return task.work();
	}
	
	public static void doPost(String url, String params, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error)
	{
		doPost(url, params, timeout, callback, error, null);
	}
	
	public static void doPost(String url, String params, long timeout, BiConsumer<Integer,String> callback, BiConsumer<Integer,String> error, ExecutorService pools)
	{
		try
		{
			if (pools != null)
			{
				pools.execute(new HttpStringPostTask(url, params, timeout, callback, error, pools));
			}
			else
			{
				Thread t = new Thread(new HttpStringPostTask(url, params, timeout, callback, error, pools));
				t.start();
			}
		}
		catch (RejectedExecutionException e1)
		{
			if (url != null)
			{
				logger.error("reject error message={} url={} params={}", e1.getMessage(), url, params);
			}
			else
			{
				logger.error("reject error message={}", e1.getMessage());
			}
			throw e1;
		}
		catch (Exception e2)
		{
			if (url != null)
			{
				logger.error("unknown error message={} url={} params={}", e2.getMessage(), url, params);
			}
			else
			{
				logger.error("unknown error message={}", e2.getMessage());
			}
			throw e2;
		}
	}
}
