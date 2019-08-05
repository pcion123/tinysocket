package com.tiny.socket.socketio.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonUtil
{
	private static Logger logger = LoggerFactory.getLogger(JsonUtil.class);
	
	public static String obj2JsonStr(Object obj)
	{
		String resultText = JSON.toJSONString(obj,
				SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullStringAsEmpty,
				SerializerFeature.QuoteFieldNames, SerializerFeature.DisableCircularReferenceDetect);
		return resultText;
	}

	public static <T> T toObject(String json, Class<T> clazz)
	{
		return JSON.parseObject(json, clazz);
	}
	
	public static JSONObject toObject(String json)
	{
		return JSON.parseObject(json);
	}
	
	public static <T> List<T> toListObject(String json, Class<T> clazz)
	{
		return JSON.parseArray(json, clazz);
	}
	
    public static Map<String, Object> toMap(String json)
    {
    	Map<String, Object> map = new HashMap<String, Object>();
    	try
    	{
    		map = toObject(json, HashMap.class);
    		for (String key : map.keySet())
    		{
    			Object value = map.get(key);
    			if (value != null)
    			{
    				String valueStr = String.valueOf(value);
    				if (valueStr.startsWith("{") && valueStr.endsWith("}"))
    				{
    					map.put(key, toMap(valueStr));
    				}
    			}
    		}
    	}
    	catch (Exception e)
    	{
    		logger.error(e.getMessage(), e);
    	}
    	return map;
   }
}
