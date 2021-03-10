package com.visionalsoftware.multilib.networking.server;

import java.util.ArrayList;

import com.visionalsoftware.multilib.networking.packet.BasicPacket.ClientQuery;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerAffirmationResponse;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerResponse;
import com.visionalsoftware.multilib.networking.packet.MultilibPacketObject;
import com.visionalsoftware.multilib.networking.server.Server.ActiveClientConnection;

public class ServerConfiguration {
	
	public static interface Authentication {
		public ServerAffirmationResponse authenticate(MultilibPacketObject information);
	}
	
	public static interface ServerProcess {
		public ServerResponse process(ClientQuery packet, 
				ArrayList<ActiveClientConnection> connections, int serverGetPort);
	}
	
	private Authentication authentication = null;
	private ServerProcess process = null;
	
	private int OBJECT_BUFFER_SIZE = (int)Math.pow(2, 10), 
		GET_PORT = 25565, 
		POST_PORT = 25566;
	private short LISTENER_DELAY = 100,
			PROCESSOR_DELAY = 100,
			SENDER_DELAY = 100;
	
	public void setObjectBufferSize(int bufferSize) { this.OBJECT_BUFFER_SIZE = bufferSize; }
	public void setGetPort(int port) { this.GET_PORT = port; }
	public void setPostPort(int port) { this.POST_PORT = port; }
	public void setListenerDelay(short delay) { this.LISTENER_DELAY = delay; }
	public void setProcessorDelay(short delay) { this.PROCESSOR_DELAY = delay; }
	public void setSenderDelay(short delay) { this.SENDER_DELAY = delay; }
	
	public void setAuthenticationProcess(Authentication auth) { this.authentication = auth; }
	public void setServerProcess(ServerProcess process) { this.process = process; }
	
	public Authentication getAuthenticationProcess() { return authentication; }
	public ServerProcess getServerProcess() { return process; }
	public short getSenderDelay() { return SENDER_DELAY; }
	public short getListenerDelay() { return LISTENER_DELAY; }
	public short getProcessorDelay() { return PROCESSOR_DELAY; }
	public int getGetPort() { return GET_PORT; }
	public int getObjectBufferSize() { return OBJECT_BUFFER_SIZE; }
	public int getPostPort() { return POST_PORT; }
	
}
