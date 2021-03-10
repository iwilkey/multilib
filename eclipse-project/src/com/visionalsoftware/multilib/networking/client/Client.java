package com.visionalsoftware.multilib.networking.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.SerializationUtils;

import com.visionalsoftware.multilib.networking.packet.BasicPacket;
import com.visionalsoftware.multilib.networking.packet.BasicPacket.ServerAffirmationResponse;
import com.visionalsoftware.multilib.networking.packet.MultilibPacketObject;

public class Client extends Thread {
	
	boolean running = false,
			tryingToConnect = false,
			affirmation = false,
			connected = false;
	DatagramSocket POST_SOCKET, GET_SOCKET;
	Thread listener, sender;
	Queue<DatagramPacket> incoming, outgoing;
	
	public ClientConfiguration config;
	
	
	long clientFaults = 0,
			lostIncomingPackets = 0,
			lostOutgoingPackets = 0,
			packetsReceived = 0,
			packetsSent = 0;
	
	public Client() {
		config = new ClientConfiguration();
	}
	
	private long nsSinceLastResponse = 0, now, last;
	public void connect(String serverIP, int serverGetPort) {
		if(!running) initialize();
		
		/*
		if(config.getClientConnectionInformation() == null) {
			log("failure", "The client connection information has not been defined so a connection is impossible!");
			return;
		}
		*/
		
		config.setServerGetPortTarget(serverGetPort);
		try {
			config.setServerAddressTarget(InetAddress.getByName(serverIP));
		} catch (UnknownHostException e) {
			log("faliure", "Cannot resolve hostname!");
			return;
		}
		
		// Send connection packet.
		enqueueQuery(new BasicPacket.ClientQuery.ConnectionRequest(config.getClientConnectionInformation(), config.getGetPort()));
	
		affirmation = false; tryingToConnect = true;
		nsSinceLastResponse = 0; now = System.nanoTime(); last = 0;
		while(true) {
			last = now;
			now = System.nanoTime();
			nsSinceLastResponse += (now - last);
			if(affirmation) {
				connected = true;
				log("success", "Successfully connected to server!");
				break;
			}
			if(nsSinceLastResponse > config.getTimeoutValue() * 1000000) {
				tryingToConnect = false;
				log("timeout", "The connection timed out.");
				break;
			}
		}
	}
	
	private void initialize() {
		try {
			POST_SOCKET = new DatagramSocket(config.getPostPort());
			GET_SOCKET = new DatagramSocket(config.getGetPort());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		incoming = new LinkedList<>();
		outgoing = new LinkedList<>(); 
		
		// Threads here.
		final byte[] clearBuffer = new byte[config.getObjectBufferSize()];
		listener = new Thread("Multilib Client Packet Listener") {
			final DatagramPacket clearIncomingPacket = new DatagramPacket(clearBuffer, clearBuffer.length);
			DatagramPacket incomingPacket;
			public void run() {
				while(true) {
					try {
						incomingPacket = clearIncomingPacket;
						try {
							GET_SOCKET.receive(incomingPacket);
						} catch (IOException e) {
							e.printStackTrace();
							lostIncomingPackets++;
							clientFaults++;
							continue;
						}
						packetsReceived++;
						log("Success", "Got a packet!");
						synchronized(incoming) {
							incoming.add(incomingPacket);
						}						
						sleep(config.getListenerDelay());
					} catch (Exception e) {
						e.printStackTrace();
						clientFaults++;
					}
				}
			}
		};
		
		
		sender = new Thread("Multilib Client Packet Sender") {
			DatagramPacket outgoingPacket = null;
			public void run() {
				while(true) {
					try {
						synchronized(outgoing) {
							while(outgoing.size() - 1 != -1) {
								outgoingPacket = outgoing.poll();
								try {
									POST_SOCKET.send(outgoingPacket);
								} catch (IOException e) {
									e.printStackTrace();
									clientFaults++;
									lostOutgoingPackets++;
									continue;
								}
								packetsSent++;
								log("success", "packet sent!");
							}
						}
						outgoing.clear();
						outgoingPacket = null;		
						sleep(config.getSenderDelay());
					} catch (Exception e) {
						e.printStackTrace();
						clientFaults++;
					}
				}
			}
		};
		
		running = true;
		listener.start(); start(); sender.start();
		log("success", "Multilib client now listening on port " + config.getGetPort() + ".");
	}
	
	public void run() {
		DatagramPacket incomingPacket = null;
		BasicPacket receivedObject = null;
		while(running) {
			try {
				synchronized(incoming) {
					while(incoming.size() - 1 != -1) {
						incomingPacket = incoming.poll();
						receivedObject = deserialize(incomingPacket.getData());
						
						if(tryingToConnect) 
							if(receivedObject instanceof ServerAffirmationResponse)
								affirmation = true;
					}
					incoming.clear();
				}
				
				sleep(config.getProcessorDelay());
			} catch (Exception e) {
				e.printStackTrace();
				clientFaults++;
			}
		}
	}
	
	// Enqueue a packet to send to the current connected server.
	private byte[] data = null;
	private DatagramPacket packet = null;
	private synchronized boolean enqueueQuery(BasicPacket.ClientQuery p) {
		try {
			data = SerializationUtils.serialize(p);
			if(data.length > config.getObjectBufferSize()) {
				log("buffer overflow", "" + Integer.toString((data.length - config.getObjectBufferSize())) + " bytes needed!\nClass: "
						+ p.getClass().getName() + "\nIncrease the buffer size to send this object.");
				return false;
			}
			if(packet == null) packet = new DatagramPacket(data, data.length, 
					config.getAddressOfServer(), config.getGetPortOfServer());
			else {
				packet.setData(data);
				packet.setLength(data.length);
				packet.setAddress(config.getAddressOfServer());
				packet.setPort(config.getGetPortOfServer());
			}
			data = null;
			synchronized(outgoing) {
				outgoing.add(packet);
			}
			
			//System.gc();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			clientFaults++;
			return false;
		}
	}
	
	private synchronized BasicPacket deserialize(byte[] stream) {
		return SerializationUtils.deserialize(stream);
	}
	
	// Simple log function.
	private void log(String tag, String message) {
		System.out.println("[MULTILIB CLIENT: " + tag.toUpperCase() + "] << " + message);
	}
	
	// Testing only.
	public static void main(String[] args) {
		Client client = new Client();
		client.config.setObjectBufferSize((int)Math.pow(2, 11));
		client.config.setClientConnectionInformation(new MultilibPacketObject());
		client.connect("localhost", 25565);
	}
	
}
