import java.io.*;
import java.net.*;
import java.nio.file.*;

// port #s 10008-10011

class Server {

	public static final int PACKET_SIZE = 128;

	public static void main(String[] args) throws Exception
	{
		int portNumber = 10008;
		DatagramSocket serverSocket = new DatagramSocket(portNumber);

		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];

		while (true) {
			DatagramPacket receivePacket = new DatagramPacket(
					receiveData, receiveData.length);
			serverSocket.receive(receivePacket);

			String sentence = new String(receivePacket.getData());

			System.out.println("Message recieved: " + sentence);

			InetAddress ipAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			
			byte[] message = Files.readAllBytes(Paths.get("message.txt"));		
			sendMessage(message, ipAddress, clientPort, serverSocket);
		}
	}

	private static byte[] nextPacket(byte[] message, int readHead) {
		byte[] packet = new byte[PACKET_SIZE];
		for (int i = 0; i < packet.length; i++) {
			if (readHead + i == message.length) break;
			packet[i] = message[readHead + i];
		}
		return packet;
	}

	private static void sendMessage(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
	    int readHead = 0;
	    while (readHead < message.length) {
	      byte[] packet = nextPacket(message, readHead);
	      DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
	      socket.send(sendPacket);
	      readHead += PACKET_SIZE;
	    }
		byte[] nullPacket  = new byte[1];
		nullPacket[0] = 0;
		DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
	    socket.send(sendPacket);

	}

	private int errorCheck() {
		return 0;
	}
}

