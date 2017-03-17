import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.ByteBuffer;

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

			byte[] message = concat(httpHeader(), Files.readAllBytes(Paths.get("TestFile.html")));
			sendMessage(message, ipAddress, clientPort, serverSocket);
		}
	}

	private static byte[] httpHeader() {
		String out = "HTTP/1.0 Document Follows \r\n";
		out += "Content-Type: text/plain\r\n";
		out += "Content-Length: xxx\r\n\r\n";
		return out.getBytes();
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] out = new byte[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	private static byte[] nextPacket(byte[] message, int readHead) {
		int checksum = sumBytesInPacket(message);
		byte[] checksumByteArray = ByteBuffer.allocate(4).putInt(checksum).array();

		byte[] packet = new byte[PACKET_SIZE];
		packet[0] = checksumByteArray[0];
		packet[1] = checksumByteArray[1];

		for (int i = 3; i < packet.length; i++) {
			if (readHead + i == message.length) break;
			packet[i] = message[readHead + i];
		}
		return packet;
	}

	private static void sendMessage(byte[] message, InetAddress ipAddr, int port, DatagramSocket socket) throws Exception {
	    int readHead = 0;
	    while (readHead < message.length+1) {
	      byte[] packet = nextPacket(message, readHead);
	      DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, ipAddr, port);
	      socket.send(sendPacket);
	      readHead += PACKET_SIZE - 4;
	    }
		byte[] nullPacket  = new byte[1];
		nullPacket[0] = 0;
		DatagramPacket sendPacket = new DatagramPacket(nullPacket, 1, ipAddr, port);
	    socket.send(sendPacket);
	}

	private static int sumBytesInPacket(byte[] packet) {
		int total = 0;
		for (byte b : packet) total += b;
		return total;
	}
}
