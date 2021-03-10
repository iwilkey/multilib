package com.visionalsoftware.multilib.networking.example;

import java.util.ArrayList;

import com.visionalsoftware.multilib.networking.client.Client;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ClientQuery;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerAffirmationResponse;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerResponse;
import com.visionalsoftware.multilib.networking.packet.MultilibPacketObject;
import com.visionalsoftware.multilib.networking.server.Server;
import com.visionalsoftware.multilib.networking.server.Server.ActiveClientConnection;
import com.visionalsoftware.multilib.networking.server.ServerConfiguration.Authentication;
import com.visionalsoftware.multilib.networking.server.ServerConfiguration.ServerProcess;

public class BasicSystem {
	
	public static class ServerResponseObject extends MultilibPacketObject {
		private static final long serialVersionUID = -3400412398731038079L;
		public String message = "You're in!";
	}
	
	static Server server;
	static Client client;
	
	public static void main(String[] args) {
		
		server = new Server();
		server.config.setObjectBufferSize((int)Math.pow(2, 11));
		
		server.config.setAuthenticationProcess(new Authentication() {
			@Override
			public ServerAffirmationResponse authenticate(MultilibPacketObject information) {
				
				// Use the information provided to tell if the person can join or not.
				
				return new ServerAffirmationResponse(new ServerResponseObject());
				
				// If null is returned, the person can't join.
				// return null;
			}
			
		});
		
		server.config.setServerProcess(new ServerProcess() {
			@Override
			public ServerResponse process(ClientQuery packet, ArrayList<ActiveClientConnection> allCurrentActiveConnections, int serverGetPort) {
				
				// Server response needs contents... (Object)
				// Server response need recipients... (ArrayList<ActiveClientConnections>)
				// Server response needs serverGetPort... (Given)
				ServerResponseObject responseContents = new ServerResponseObject();
				
				// This sends an empty object to all connected clients.
				return new ServerResponse(responseContents, allCurrentActiveConnections, serverGetPort);
			}
			
		});
		
		server.initialize();
		
		client = new Client();
		client.config.getClientConnectionInformation();
		client.connect("localhost", 25565);
	}

}
