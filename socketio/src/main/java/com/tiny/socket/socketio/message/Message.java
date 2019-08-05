package com.tiny.socket.socketio.message;

import com.tiny.socket.socketio.buffer.ByteArrayBuffer;
import com.tiny.socket.socketio.header.HeaderBase;

import io.netty.buffer.ByteBuf;

public class Message
{
	private HeaderBase header;
	private ByteArrayBuffer buffer;
	
	public Message(HeaderBase header, ByteArrayBuffer buffer)
	{
		this.header = header;
		this.buffer = buffer;
	}
	
	public Message(HeaderBase header, ByteBuf buffer)
	{
		this.header = header;
		this.buffer = new ByteArrayBuffer(buffer);
	}
	
	public Message(HeaderBase header, byte[] buffer)
	{
		this.header = header;
		this.buffer = new ByteArrayBuffer(buffer);
	}
	
	@Override
	public String toString()
	{
		return header.toString();
	}
	
	public boolean release()
	{
		return buffer != null ? buffer.release() : false;
	}
	
	public Message clone()
	{
		return new Message(header.clone(), buffer.clone());
	}
	
	public <T extends HeaderBase> T getHeader(Class<T> clazz)
	{
		return clazz.cast(header);
	}
	
	public HeaderBase getHeader()
	{
		return header;
	}
	
	public void setHeader(HeaderBase header)
	{
		this.header = header;
	}
	
	public ByteArrayBuffer getBuffer()
	{
		return buffer;
	}
	
	public void setBuffer(ByteArrayBuffer buffer)
	{
		this.buffer = buffer;
	}
}
