package com.tiny.socket.socketio.util;

import java.util.List;

public class StringUtil
{
	public static boolean isNotNullOrNotEmpty(String string)
	{
		return (string != null && string.length() > 0);
	}

	public static boolean isNullOrEmpty(String string)
	{
		return (string == null || string.length() == 0);
	}
	
	public static boolean isNotNullOrNotEmpty(List list)
	{
		return (list != null && list.size() > 0);
	}
	
	public static int getActualLength(String string)
	{
		return string != null ? string.getBytes().length : 0;
	}
	
    public static String padRight(String src, int len, char ch)
    {
        int diff = len - src.length();
        
        if (diff <= 0)
        	return src;

        char[] charr = new char[len];
        
        System.arraycopy(src.toCharArray(), 0, charr, 0, src.length());
        
        for (int i = src.length(); i < len; i++)
        	charr[i] = ch;
        
        return new String(charr);
    }
	
	public static String padLeft(String src, int len, char ch)
	{
        int diff = len - src.length();
        
        if (diff <= 0)
        	return src;

        char[] charr = new char[len];
        
        System.arraycopy(src.toCharArray(), 0, charr, diff, src.length());
        
        for (int i = 0; i < diff; i++)
        	charr[i] = ch;
        
        return new String(charr);
	}
	
	public static void main(String[] args)
	{
		String source = "pcion123";
		String value1 = StringUtil.padLeft(source, 32, '0');
		String value2 = StringUtil.padRight(source, 32, '0');
		System.out.println(String.format("source=%s   value1=%s   value2=%s", source, value1, value2));
	}
}
