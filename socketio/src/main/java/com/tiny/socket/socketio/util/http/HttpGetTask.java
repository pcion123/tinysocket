package com.tiny.socket.socketio.util.http;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tiny.socket.socketio.util.HttpUtil;

public class HttpGetTask implements Runnable
{
	private static Logger logger = LoggerFactory.getLogger(HttpGetTask.class);
	
	private boolean running = true;
	private long timeout;
	private Task task;
	private ExecutorService pools;
	private BiConsumer<Integer,String> error;
	
	public HttpGetTask(String url, long timeout, BiConsumer<Integer,String> finish, BiConsumer<Integer,String> error, ExecutorService pools)
	{
		this.timeout = timeout;
		this.task = new Task(url, timeout, finish);
		this.pools = pools;
		this.error = error;
	}
	
	public boolean running()
	{
		return running;
	}
	
	public void shutdown()
	{
    	try
    	{
        	if (task != null)
        		task.shutdown();
        	
        	running = false;
    	}
    	catch (Exception e)
    	{
    		logger.error(e.getMessage(), e);
    	}
	}
	
	@SuppressWarnings("static-access")
	public String work()
	{
		try
		{
			if (pools != null)
			{
				pools.execute(task);
			}
			else
			{
				Thread t = new Thread(task);
				t.start();
			}
			long timestamp = System.currentTimeMillis();
			while(task.running())
			{
				long now = System.currentTimeMillis();
				if (now - timestamp > timeout)
					task.shutdown();
				Thread.currentThread().sleep(100);
			}
		}
		catch (RejectedExecutionException e1)
		{
			if (task != null && task.getUrl() != null)
			{
				logger.error("reject error message={} url={}", e1.getMessage(), task.getUrl());
			}
			else
			{
				logger.error("reject error message={}", e1.getMessage());
			}
			throw e1;
		}
		catch (Exception e2)
		{
			if (task != null && task.getUrl() != null)
			{
				logger.error("unknown error message={} url={}", e2.getMessage(), task.getUrl());
			}
			else
			{
				logger.error("unknown error message={}", e2.getMessage());
			}
		}
        return task.getResult();
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void run()
	{
		running = true;
		try
		{
			if (pools != null)
			{
				pools.execute(task);
			}
			else
			{
				Thread t = new Thread(task);
				t.start();
			}
			long timestamp = System.currentTimeMillis();
			while(task.running())
			{
				long now = System.currentTimeMillis();
				if (now - timestamp > timeout)
					task.shutdown();
				Thread.currentThread().sleep(100);
			}
		}
		catch (RejectedExecutionException e1)
		{
			if (task != null && task.getUrl() != null)
			{
				logger.error("reject error message={} url={}", e1.getMessage(), task.getUrl());
			}
			else
			{
				logger.error("reject error message={}", e1.getMessage());
			}
	        if (error != null)
	        	error.accept(11, e1.getMessage());
		}
		catch (Exception e2)
		{
			if (task != null && task.getUrl() != null)
			{
				logger.error("unknown error message={} url={}", e2.getMessage(), task.getUrl());
			}
			else
			{
				logger.error("unknown error message={}", e2.getMessage());
			}
	        if (error != null)
	        	error.accept(12, e2.getMessage());
		}
        running = false;
	}
	
	private class Task implements Runnable
	{
		private boolean running = true;
		private int status = 999;
		private String result = null;
		private CloseableHttpClient client;
		private CloseableHttpResponse response;
		private String url;
		private long timeout;
		private BiConsumer<Integer,String> finish;
		
		public Task(String url, long timeout, BiConsumer<Integer,String> finish)
		{
			this.url = url;
			this.timeout = timeout;
			this.finish = finish;
		}
		
		public String getUrl()
		{
			return url;
		}
		
		@SuppressWarnings("unused")
		public int getStatus()
		{
			return status;
		}
		
		public String getResult()
		{
			return result;
		}
		
		public boolean running()
		{
			return running;
		}
		
		public void shutdown()
		{
	    	try
	    	{
	        	if (response != null)
	        		response.close();
	        	
	        	running = false;
	    	}
	    	catch (Exception e)
	    	{
	    		logger.error(e.getMessage(), e);
	    	}
		}
		
		@Override
		public void run()
		{
			running = true;
			
			status = 999;
			result = null;
	        try
	        {
	        	client = HttpUtil.builder.build();
	        	HttpGet request = new HttpGet(url);
	        	if (timeout != 0)
	        	{
		            RequestConfig config = RequestConfig.custom()
		            		.setConnectTimeout((int)timeout)
		            		.setConnectionRequestTimeout((int)timeout)
		            		.setSocketTimeout((int)timeout).build();
		            request.setConfig(config);
	        	}
	        	response = client.execute(request);
	        	status = response.getStatusLine().getStatusCode();
	        	if (status == HttpStatus.SC_OK)
	        		result = EntityUtils.toString(response.getEntity());
	        }
	        catch (IllegalStateException e1)
	        {
	        	status = 998;
	        	result = null;
	        }
	        catch (HttpHostConnectException e2)
	        {
	        	status = 997;
	        	result = null;
	        }
	        catch (SocketTimeoutException e3)
	        {
	        	status = 996;
	        	result = null;
	        }
	        catch (Exception e4)
	        {
	        	status = 999;
	        	result = null;
	        	logger.error(e4.getMessage(), e4);
	        }
	        finally
	        {
	        	try
	        	{
	        		if (response != null)
	        			response.close();
	        	}
	        	catch (Exception e)
	        	{
	        		logger.error(e.getMessage(), e);
	        	}
	        }
	        
	        if (finish != null)
	        	finish.accept(status, result);
	        
	        running = false;
		}
	}
}