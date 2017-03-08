import java.io.*;
import java.net.*;

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

			//System.out.println("Message recieved: " + sentence);

			InetAddress ipAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			String capitalizeSentence = sentence.toUpperCase();
			sendData = capitalizeSentence.getBytes();

			DatagramPacket sendPacket = new DatagramPacket(
				sendData, sendData.length, ipAddress, clientPort);

			serverSocket.send(sendPacket);

		}

	}

	private byte[] nextPacket(byte[] message, int readHead) {
		byte[] packet;
		if (message.length <= readHead + PACKET_SIZE) {
			packet = new byte[message.length - readHead];
		} else {
			packet = new byte[PACKET_SIZE];
		}
		for (int i = 0; i < packet.length; i++) {
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
	}

	private int computeChecksum() {
		return 0;
	}
}
