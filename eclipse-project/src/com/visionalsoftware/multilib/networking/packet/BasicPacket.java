package com.visionalsoftware.multilib.networking.packet;

import java.io.Serializable;
import java.util.ArrayList;

import com.visionalsoftware.multilib.networking.server.Server;

// All packets must extend this class.
public abstract class BasicPacket implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int getPort;
	protected MultilibPacketObject contents;
	
	public BasicPacket(int getPort) { this.getPort = getPort; }
	public int getGetPort() { return getPort; }
	public MultilibPacketObject getContents() { return contents; }
	
	// A question a client asks a server. Can (and should) be expanded upon to fit specific program functionality.
	public static class ClientQuery extends BasicPacket {
		private static final long serialVersionUID = 1L;
		public ClientQuery(MultilibPacketObject queryContents, int getPort) {
			super(getPort);
			contents = queryContents;
		}
		public MultilibPacketObject getQueryContents() { return contents; }
		
		public static class ConnectionRequest extends ClientQuery {
			private static final long serialVersionUID = 1L;
			public ConnectionRequest(MultilibPacketObject information, int getPort) { 
				super(information, getPort);
			}
		}
		
		public static class DisconnectionRequest extends ClientQuery {
			private static final long serialVersionUID = 1L;
			public DisconnectionRequest(int getPort) {
				super(null, getPort);
			}
		}
	}
	
	// Sent by a server as a response to a ClientQuery.
	public static class ServerResponse extends BasicPacket {
		private static final long serialVersionUID = 1L;
		ArrayList<Server.ActiveClientConnection> recipients;
		public ServerResponse(MultilibPacketObject responseContents, ArrayList<Server.ActiveClientConnection> recipients, int getPort) {
			super(getPort);
			contents = responseContents;
			this.recipients = recipients;
		}
		public ArrayList<Server.ActiveClientConnection> getRecipients() { return recipients; }
		public MultilibPacketObject getResponseContents() { return contents; }
	}
	
	public static class ServerAffirmationResponse extends ServerResponse {
		private static final long serialVersionUID = 1L;
		public ServerAffirmationResponse(MultilibPacketObject welcomePacketContents) {
			super(welcomePacketContents, null, 0);
			contents = welcomePacketContents;
		}
		public MultilibPacketObject getWelcomePacketContents() { return contents; }
	}

}
