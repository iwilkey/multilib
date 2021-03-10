package com.visionalsoftware.multilib.networking.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.SerializationUtils;

import com.visionalsoftware.multilib.networking.packet.BasicPacket;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerAffirmationResponse;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerResponse;
import com.visionalsoftware.multilib.networking.packet.MultilibPacketObject;

public class Server extends Thread {
	
	public static class ActiveClientConnection {
		InetAddress address;
		int postPort, getPort;
		public ActiveClientConnection(InetAddress address, int post, int get) {
			this.address = address;
			this.postPort = post;
			this.getPort = get;
		}
		public InetAddress getAddress() { return this.address; }
		public int getPostPort() { return this.postPort; }
		public int getGetPort() { return this.getPort; }
	}
	
	private boolean running = false;
	private DatagramSocket POST_SOCKET, GET_SOCKET;
	public ServerConfiguration config;
	private Thread listener, sender;
	private ArrayList<ActiveClientConnection> connections;
	private Queue<DatagramPacket> incoming, outgoing;
	
	long serverFaults = 0,
		lostIncomingPackets = 0,
		lostOutgoingPackets = 0,
		packetsReceived = 0,
		packetsSent = 0;

	public Server() {
		config = new ServerConfiguration();
	}
	
	public void initialize() {
		try {
			POST_SOCKET = new DatagramSocket(config.getPostPort());
			GET_SOCKET = new DatagramSocket(config.getGetPort());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		connections = new ArrayList<>();
		incoming = new LinkedList<>();
		outgoing = new LinkedList<>();
		
		if(config.getAuthenticationProcess() == null) log("warning", "The authentication process is not defined so this server is not very secure!");
		if(config.getServerProcess() == null) log("warning", "The server process is not defined so this server will have no functionality!");
		
		final byte[] clearBuffer = new byte[config.getObjectBufferSize()];
		listener = new Thread("Multilib Server Packet Listener") {
			final DatagramPacket clearIncomingPacket = new DatagramPacket(clearBuffer, clearBuffer.length);
			DatagramPacket incomingPacket;
			public void run() {
				while(running) {
					try {
						incomingPacket = clearIncomingPacket;
						try {
							GET_SOCKET.receive(incomingPacket);
						} catch (IOException e) {
							e.printStackTrace();
							lostIncomingPackets++;
							serverFaults++;
							continue;
						}
						packetsReceived++;
						log("success", "Got a packet!");
						synchronized(incoming) {
							incoming.add(incomingPacket);
						}
						sleep(config.getListenerDelay());
					} catch (Exception e) {
						e.printStackTrace();
						serverFaults++;
					}

				}
			}
		};
		
		sender = new Thread("Multilib Server Packet Sender") {
			DatagramPacket outgoingPacket = null;
			public void run() {
				while(running) {
					try {
						synchronized(outgoing) {
							while(outgoing.size() - 1 != -1) {
								outgoingPacket = outgoing.poll();
								
								try {
									POST_SOCKET.send(outgoingPacket);
								} catch (IOException e) {
									e.printStackTrace();
									serverFaults++;
									lostOutgoingPackets++;
									continue;
								}
								packetsSent++;
								log("success", "Sent a packet.");
							}
						}
						outgoing.clear();
						outgoingPacket = null;
						sleep(config.getSenderDelay());
					} catch (Exception e) {
						e.printStackTrace();
						serverFaults++;
					}
				}
			}
		};
		
		running = true;
		listener.start(); start(); sender.start(); 
		
		log("success", "Multilib server now listening on port " + config.getGetPort() + ".");
	}
	
	public void run() {
		DatagramPacket incomingPacket = null;
		BasicPacket receivedObject = null;
		ServerResponse response = null;
		MultilibPacketObject contents = null;
		ServerAffirmationResponse serverAffirmation = null;
		while(running) {
			try {
				
				synchronized(incoming) {
					while(incoming.size() - 1 != -1) {
						incomingPacket = incoming.poll();
						receivedObject = deserialize(incomingPacket.getData());
						
						if(!(receivedObject instanceof BasicPacket.ClientQuery)) continue;
						synchronized(connections) {
							if(clientConnected(incomingPacket.getAddress(), incomingPacket.getPort())) {
								// The client was recognized as an active connection to the server...
								
								// Handle disconnection requests...
								if(receivedObject instanceof BasicPacket.ClientQuery.DisconnectionRequest) {
									removeConnection(incomingPacket.getAddress(), incomingPacket.getPort(), receivedObject.getGetPort());
								} else {
									// Process data here...
									if(config.getServerProcess() != null) {
										response = config.getServerProcess().process((BasicPacket.ClientQuery)receivedObject, connections, config.getGetPort());
										for(ActiveClientConnection c : response.getRecipients())
											enqueueResponse(response, c);
									}
								}
							
							} else {
								// Handle connection requests...
								if(receivedObject instanceof BasicPacket.ClientQuery.ConnectionRequest) {
									// This client wants to join, so run authentication process...
									if(config.getAuthenticationProcess() != null) {
										contents = ((BasicPacket.ClientQuery.ConnectionRequest)receivedObject).getQueryContents();
										serverAffirmation = config.getAuthenticationProcess().authenticate(contents);
										if(serverAffirmation != null) {
											addConnection(incomingPacket.getAddress(), incomingPacket.getPort(), receivedObject.getGetPort());
											enqueueResponse(serverAffirmation, returnConnection(incomingPacket.getAddress(), incomingPacket.getPort(), receivedObject.getGetPort()));
										}
									} else {
										addConnection(incomingPacket.getAddress(), incomingPacket.getPort(), receivedObject.getGetPort());
									}
								}
							}
						}
					}
					incoming.clear();
				}
				
				receivedObject = null; incomingPacket = null; response = null; contents = null; serverAffirmation = null;
				sleep(config.getProcessorDelay());
			} catch (Exception e) {
				e.printStackTrace();
				serverFaults++;
			}
		}
	}
	
	// A client is "actively connected" when they have passed the authentication process.
	private synchronized boolean clientConnected(InetAddress address, int postPort) {
		for(ActiveClientConnection c : connections) 
			if(c.getAddress().equals(address) && c.getPostPort() == postPort) return true;
		return false;
	}
	
	// Add the client to the connection list because they passed the authentication process.
	private synchronized void addConnection(InetAddress address, int postPort, int getPort) {
		connections.add(new ActiveClientConnection(address, postPort, getPort));
	}
	
	private synchronized ActiveClientConnection returnConnection(InetAddress address, int postPort, int getPort) {
		for(ActiveClientConnection c : connections) 
			if(c.address.equals(address) && c.postPort == postPort && c.getPort == getPort) return c;
		return null;
	}
	
	// Remove a connection.
	private synchronized void removeConnection(InetAddress address, int postPort, int getPort) {
		connections.remove(new ActiveClientConnection(address, postPort, getPort));
	}
	
	// Return an object version of what was received by the server.
	private synchronized BasicPacket deserialize(byte[] stream) {
		return SerializationUtils.deserialize(stream);
	}
	
	// Enqueue a packet to send to a specific connection.
	private byte[] data = null;
	private DatagramPacket packet = null;
	private synchronized boolean enqueueResponse(BasicPacket.ServerResponse p, ActiveClientConnection c) {
		try {
			data = SerializationUtils.serialize(p);
			if(data.length > config.getObjectBufferSize()) {
				log("buffer overflow", "" + Integer.toString((data.length - config.getObjectBufferSize())) + " bytes needed!\nClass: "
						+ p.getClass().getName() + "\nIncrease the buffer size to send this object.");
				return false;
			}
			if(packet == null) packet = new DatagramPacket(data, data.length, c.getAddress(), c.getGetPort());
			else {
				packet.setData(data);
				packet.setLength(data.length);
				packet.setAddress(c.getAddress());
				packet.setPort(c.getGetPort());
			}
			data = null;
			synchronized(outgoing) {
				outgoing.add(packet);
			}
			
			//System.gc();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			serverFaults++;
			return false;
		}
	}
	
	// Simple log function.
	private void log(String tag, String message) {
		System.out.println("[MULTILIB SERVER: " + tag.toUpperCase() + "] << " + message);
	}
	
	// Testing only.
	public static void main(String args[]) {
		Server server = new Server();
		server.config.setObjectBufferSize((int)Math.pow(2, 11));
		server.initialize(); // Start the server after configuration.
	}
	
}
