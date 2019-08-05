package com.tiny.socket.socketio.util;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AesUtil
{
	private static Logger logger = LoggerFactory.getLogger(AesUtil.class);
	
	public static String encrypt(String data, String key)
	{
		try
		{
		    SecretKeySpec secretKeySpec = new SecretKeySpec(getKeyNormalization(key), "AES");
		    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
		    byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
		    return binary2hex(encrypted);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public static String decrypt(String data, String key)
	{
	    try
	    {
	          SecretKeySpec secretKeySpec = new SecretKeySpec(getKeyNormalization(key), "AES");
	          Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	          cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
              byte[] decrypted = cipher.doFinal(hex2binary(data));
              return new String(decrypted,"UTF-8");
	    }
	    catch (Exception e)
	    {
	    	logger.error(e.getMessage(), e);
	    }
	    return null;
	}
	
	private static byte[] getKeyNormalization(String key) throws UnsupportedEncodingException
	{
		if (key == null || key.length() == 0)
			throw new IllegalArgumentException("Aes密鑰不能為空");
		
		if (key.length() < 32)
		{
			int diff = 32 - key.length();
			char[] charr = new char[32];
			System.arraycopy(key.toCharArray(), 0, charr, 0, key.length());
			for (int i = key.length(); i < 32; i++)
				charr[i] = '0';
			key = new String(charr);
		}
		
		if (key.length() > 32)
		{
			key.substring(0, 32);
		}
		
		return key.getBytes("UTF-8");
	}
	
	private static String binary2hex(byte buf[])
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++)
		{
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if (hex.length() == 1)
				hex = '0' + hex;
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}
	
	private static byte[] hex2binary(String hexStr)
	{
		if (hexStr.length() < 1)
			return null;
		
		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++)
		{
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte)(high * 16 + low);
		}
		return result;
	}
}
