package com.tiny.socket.socketio.util;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Base64Util
{
	private static Logger logger = LoggerFactory.getLogger(Base64Util.class);
	
	private final static Encoder encoder = Base64.getEncoder();
	private final static Decoder decoder = Base64.getDecoder();
	
	public static String encrypt(String data)
	{
		try
		{
			return encrypt(data.getBytes("UTF-8"));
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static String encrypt(String data, int key)
	{
		try
		{
			return encrypt(data.getBytes("UTF-8"), key);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static String encrypt(byte[] data)
	{
		return encrypt(data, 0);
	}
	
	public static String encrypt(byte[] data, int key)
	{
		if (key > 0)
		{
			for (int i = 0; i < data.length; i++)
				data[i] = (byte)(data[i] ^ key);
			return encoder.encodeToString(data);
		}
		else
		{
			return encoder.encodeToString(data);
		}
	}
	
	public static String decrypt(String data)
	{
		return decrypt(data, 0);
	}
	
	public static String decrypt(String data, int key)
	{
		try
		{
			if (key > 0)
			{
				byte[] array = decoder.decode(data);
				for (int i = 0; i < array.length; i++)
					array[i] = (byte)(array[i] ^ key);
				return new String(array, "UTF-8");
			}
			else
			{
				return new String(decoder.decode(data), "UTF-8");
			}
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		return null;
	}
}
