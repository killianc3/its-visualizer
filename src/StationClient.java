import org.pcap4j.core.Pcaps;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.Packet;
import org.pcap4j.core.PcapHandle;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

class StationClient {
	public static void main(String[] args) {
		List<PcapNetworkInterface> networkInterfaces;
		try {
			networkInterfaces = Pcaps.findAllDevs();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		var index = 0;
		for (var networkInterface : networkInterfaces) {
			System.out.println(index++ + " : " + networkInterface.getName() + " : " + networkInterface.getDescription());
		}

		long providerStationId;
		PcapNetworkInterface networkInterface;
		InetAddress serverIp;
		int serverPort;

		try {
			providerStationId = Long.parseLong(args[0]);
			networkInterface = networkInterfaces.get(Integer.parseInt(args[1]));
			serverIp = InetAddress.getByName(args[2]);
			serverPort = Integer.parseInt(args[3]);
		} catch (Exception e) {
			System.err.println("Wrong parameters, use ./runStationClient (station id) (network interface index) (server ip) (server port)");
			return;
		}

		PcapHandle pcapHandle;
		try {
			pcapHandle = networkInterface.openLive(64 * 1024, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 1);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Packet pcapPacket;

		DatagramSocket udpSocket;
		try {
			udpSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while (true) {
			try {
				if ((pcapPacket = pcapHandle.getNextPacket()) != null) {
					var packetBytes =  pcapPacket.getRawData();

					if ((((packetBytes[12] & 0xFF) << 8) | (packetBytes[13] & 0xFF)) == 0x8947) {
						var sniffPacket = new SniffPacket(pcapHandle.getTimestamp().getTime(), providerStationId, packetBytes);
						
						try {
							var baos = new ByteArrayOutputStream();
							var oos = new ObjectOutputStream(baos);
							oos.writeObject(sniffPacket);
							oos.flush();

							var sniffPacketBytes = baos.toByteArray();

							var udpPacket = new DatagramPacket(sniffPacketBytes, sniffPacketBytes.length, serverIp, serverPort);
							udpSocket.send(udpPacket);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}