package com.tiny.socket.serversocket.connection;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tiny.socket.socketio.buffer.ByteArrayBuffer;
import com.tiny.socket.socketio.util.DateUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public abstract class ConnectionBase
{
	protected static Logger logger = LoggerFactory.getLogger(ConnectionBase.class);
	
	protected Channel channel;
	protected short version;
	protected long sessionId;
	protected long connectTime;
	
	protected ConnectionBase()
	{
		this(null, (short)0, 0L, 0L);
	}
	
	protected ConnectionBase(Channel channel, short version, long sessionId, long connectTime)
	{
		this.channel = channel;
		this.version = version;
		this.sessionId = sessionId;
		this.connectTime = connectTime;
	}
	
	@Override
	public String toString()
	{
		return String.format("Version=%d Id=%s SessionId=%d Address=%s ConnectTime=%s", getVersion(), getId(), getSessionId(), getAddress(), getConnectTime());
	}
	
	protected <T> T getProperty(Class<T> clazz, Channel channel, String property)
	{
		AttributeKey<T> k = AttributeKey.valueOf(property);
		if (!channel.hasAttr(k))
		{
			return null;
		}
		Attribute<T> v = channel.attr(k);
		return v.get();
	}
	
	protected <T> void setProperty(Class<T> clazz, Channel channel, String property, T value)
	{
		AttributeKey<T> k = AttributeKey.valueOf(property);
		Attribute<T> v = channel.attr(k);
		v.set(value);
	}
	
	public Channel getChannel()
	{
		return channel;
	}
	
	public void setChannel(Channel channel)
	{
		this.channel = channel;
	}
	
	public short getVersion()
	{
		return version;
	}
	
	public void setVersion(short version)
	{
		this.version = version;
		
		if (channel != null)
		{
			setProperty(short.class, channel, "version", version);
		}
	}
	
	public long getSessionId()
	{
		return sessionId;
	}
	
	public void setSessionId(long sessionId)
	{
		this.sessionId = sessionId;
		
		if (channel != null)
		{
			setProperty(long.class, channel, "sessionId", sessionId);
		}
	}
	
	public String getConnectTime()
	{
		return DateUtil.getCurrentDateTime(connectTime);
	}
	
	public void setConnectTime(long connectTime)
	{
		this.connectTime = connectTime;
	}
	
	public ChannelId getId()
	{
		return channel != null ? channel.id() : null;
	}
	
	public String getAddress()
	{
		String address = null;
		if (channel != null)
		{
			InetSocketAddress insocket = (InetSocketAddress)channel.remoteAddress();
			String ip = insocket.getAddress().toString().split("/")[1];
			int port = insocket.getPort();
			address = String.format("%s:%d", ip, port);
		}
		return address;
	}
	
	public String getIp()
	{
		String ip = null;
		if (channel != null)
		{
			InetSocketAddress insocket = (InetSocketAddress)channel.remoteAddress();
			ip = insocket.getAddress().toString().split("/")[1];
		}
		return ip;
	}
	
	public int getPort()
	{
		int port = 0;
		if (channel != null)
		{
			InetSocketAddress insocket = (InetSocketAddress)channel.remoteAddress();
			port = insocket.getPort();
		}
		return port;
	}
	
	public void disconnect()
	{
		if (channel != null)
		{
			channel.close();
		}
	}
	
	public void send(int mainNo, int subNo, ByteArrayBuffer buffer)
	{
		send(mainNo, subNo, buffer, true);
	}
	
	public void send(int mainNo, int subNo, ByteArrayBuffer buffer, boolean release)
	{
		send((byte)mainNo, (byte)subNo, buffer, release);
	}
	
	protected abstract void send(byte mainNo, byte subNo, ByteArrayBuffer buffer, boolean release);
}
