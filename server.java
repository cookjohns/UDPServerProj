import java.io.*;
import java.net.*;

// port #s 10008-10011

class Server {
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

			InetAddress ipAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			String capitalizeSentence = sentence.toUpperCase();
			sendData = capitalizeSentence.getBytes();

			DatagramPacket sendPacket = new DatagramPacket(
				sendData, sendData.length, ipAddress, clientPort);

			serverSocket.send(sendPacket);

		}

	}

	private int computeChecksum() {
		return 0;
	}
}
