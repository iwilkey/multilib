package com.visionalsoftware.multilib.networking.client;

import java.net.InetAddress;

import com.visionalsoftware.multilib.networking.packet.MultilibPacketObject;

public class ClientConfiguration {
	
	private int OBJECT_BUFFER_SIZE = (int)Math.pow(2, 10), 
			GET_PORT = 8080, 
			POST_PORT = 8081;
	private short LISTENER_DELAY = 100,
			PROCESSOR_DELAY = 100,
			SENDER_DELAY = 100;
	private long TIMEOUT_VALUE = 5000;
	private MultilibPacketObject CLIENT_CONNECTION_INFORMATION = null;
	
	// Connection status
	private InetAddress CURRENT_CONNECTED_SERVER_ADDRESS = null;
	private int CURRENT_CONNECTED_SERVER_GET_PORT = -1;
	
	public void setObjectBufferSize(int bufferSize) { OBJECT_BUFFER_SIZE = bufferSize; }
	public void setGetPort(int port) { GET_PORT = port; }
	public void setPostPort(int port) { POST_PORT = port; }
	public void setServerAddressTarget(InetAddress address) { CURRENT_CONNECTED_SERVER_ADDRESS = address; }
	public void setServerGetPortTarget(int port) { CURRENT_CONNECTED_SERVER_GET_PORT = port; }
	public void setListenerDelay(short delay) { LISTENER_DELAY = delay; }
	public void setProcessorDelay(short delay) { PROCESSOR_DELAY = delay; }
	public void setSenderDelay(short delay) { SENDER_DELAY = delay; }
	public void setTimeoutValue(long value) { TIMEOUT_VALUE = value ; }
	public void setClientConnectionInformation(MultilibPacketObject information) { CLIENT_CONNECTION_INFORMATION = information; }
	
	public short getSenderDelay() { return SENDER_DELAY; }
	public short getListenerDelay() { return LISTENER_DELAY; }
	public short getProcessorDelay() { return PROCESSOR_DELAY; }
	public int getGetPort() { return GET_PORT; }
	public int getObjectBufferSize() { return OBJECT_BUFFER_SIZE; }
	public int getPostPort() { return POST_PORT; }
	public InetAddress getAddressOfServer() { return CURRENT_CONNECTED_SERVER_ADDRESS; }
	public int getGetPortOfServer() { return CURRENT_CONNECTED_SERVER_GET_PORT; }
	public long getTimeoutValue() { return TIMEOUT_VALUE; }
	public MultilibPacketObject getClientConnectionInformation() { return CLIENT_CONNECTION_INFORMATION; }
	
}
