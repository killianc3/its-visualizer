import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.Socket;
import java.net.ServerSocket;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Server {
	static List<TcpClient> tcpClients = new ArrayList<>();

	static class TcpClient {
		Socket tcpSocket;
		ObjectOutputStream oos;

		TcpClient(Socket tcpSocket) throws Exception {
			this.tcpSocket = tcpSocket;
			oos = new ObjectOutputStream(tcpSocket.getOutputStream());
		}
	}

	public static void main(String[] args) {
		new Thread(() -> {
			ServerSocket tcpServerSocket;
			try {
				tcpServerSocket = new ServerSocket(9999);
				System.out.println("Tcp server started at : " + tcpServerSocket.getInetAddress().getHostAddress());
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			while (true) {
				try {
					var tcpClient = new TcpClient(tcpServerSocket.accept());

					synchronized (tcpClients) {
						tcpClients.add(tcpClient);
					}

					System.out.println("New tcp client connected");
				} catch (Exception e) {}
			}
		}).start();

		new Thread(() -> {
			DatagramSocket udpServerSocket;
			try {
				udpServerSocket = new DatagramSocket(9999);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}

			var udpBuffer = new byte[1024 * 64];
			var udpPacketBuffer = new DatagramPacket(udpBuffer, udpBuffer.length);
		
			while (true) {
				try {
					udpServerSocket.receive(udpPacketBuffer);
					var udpPayload = Arrays.copyOfRange(udpPacketBuffer.getData(), 0, udpPacketBuffer.getLength());

					var bais = new ByteArrayInputStream(udpPayload);
					var ois = new ObjectInputStream(bais);

					var sniffPacket = (SniffPacket) ois.readObject();

					var disconnectedClients = new ArrayList<TcpClient>();

					for (var tcpClient : tcpClients) {
						try {
							tcpClient.oos.writeObject(sniffPacket);
							tcpClient.oos.flush();
						} catch (Exception e) {
							disconnectedClients.add(tcpClient);
						}
					}

					for (var disconnectedClient : disconnectedClients) {
						tcpClients.remove(disconnectedClient);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}