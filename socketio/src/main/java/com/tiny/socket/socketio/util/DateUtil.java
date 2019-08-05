package com.tiny.socket.socketio.util;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtil
{
	private static Logger logger = LoggerFactory.getLogger(DateUtil.class);
	
	public static java.util.Date getTimestampToDate(long timestamp)
	{
		return new java.util.Date(timestamp);
	}
	
	public static long getDateToTimestamp(java.util.Date date)
	{
		return date != null ? date.getTime() : 0;
	}
	
	public static java.util.Date getStrToDate(String timeStr)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
		{
			return simpleDateFormat.parse(timeStr);
		}
		catch (Exception e)
		{
			logger.error(String.format("getStrToDate error -> %s : %s", timeStr, e.getMessage()), e);
		}
		return null;
	}
	
	public static long getStrToTimestamp(String timeStr)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try
		{
			return simpleDateFormat.parse(timeStr).getTime();
		}
		catch (Exception e)
		{
			logger.error(String.format("getStrToTimestamp error -> %s : %s", timeStr, e.getMessage()), e);
		}
		return 0;
	}
	
	public static long getCurrentTimestamp()
	{
		return new java.util.Date().getTime();
	}
	
	public static String getCurrentDate()
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date date = new java.util.Date();
		String rst = simpleDateFormat.format(date);
		return rst;
	}
	
	public static String getCurrentDate(long timestamp)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date date = new java.util.Date(timestamp);
		String rst = simpleDateFormat.format(date);
		return rst;
	}
	
	public static String getCurrentDateTime()
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		java.util.Date date = new java.util.Date();
		String rst = simpleDateFormat.format(date);
		return rst;
	}
	
	public static String getCurrentDateTime(long timestamp)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		java.util.Date date = new java.util.Date(timestamp);
		String rst = simpleDateFormat.format(date);
		return rst;
	}
	
	public static String getCurrentTime()
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
		java.util.Date date = new java.util.Date();
		String rst = simpleDateFormat.format(date);
		return rst;
	}
	
	public static String getCurrentTime(long timestamp)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
		java.util.Date date = new java.util.Date(timestamp);
		String rst = simpleDateFormat.format(date);
		return rst;
	}
}
