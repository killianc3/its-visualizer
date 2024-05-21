import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;

import gn.GNPacket;

import net.gcdc.asn1.uper.UperEncoder;

import helper.type.HexaHelper;

import etsi.gn.GBC;
import etsi.cam.CAM;
import etsi.denm.DENM;

import java.io.Serializable;

public class PacketHandler {
	LinkedList<RawPacket> temporaryPacketBuffer;
	Map<Long, Long> macToStationIdMap;

	PacketProcessHandler packetProcessHandler;

	PacketHandler(PacketProcessHandler packetProcessHandler) {
		this.packetProcessHandler = packetProcessHandler;
		
		temporaryPacketBuffer = new LinkedList<>();
		macToStationIdMap = new HashMap<>();
	}

	public void processPacket(byte[] bytes, Long timestamp, Long providerStationId) {
		var presentInBuffer = false;

		for (var packet : temporaryPacketBuffer) {
			if (packetBytesEquals(bytes, packet.bytes)) {
				for (var reachedStation : packet.reachedStations) {
					if (reachedStation.id == providerStationId) {
						System.out.println("same provider for the same packet ");
					}
				}
				packet.reachedStations.add(new ReachedStation(providerStationId, timestamp));
				presentInBuffer = true;
				break;
			}
		}

		if (!presentInBuffer) {
			var reachedStations = new ArrayList<ReachedStation>();
			reachedStations.add(new ReachedStation(providerStationId, timestamp));

			temporaryPacketBuffer.add(new RawPacket(bytes, timestamp, reachedStations));
		}

		while (temporaryPacketBuffer.size() > 0 && timestamp - temporaryPacketBuffer.getFirst().initialTimestamp > 2000) {
			postProcessPacket(temporaryPacketBuffer.remove());
		}		
	}

	boolean packetBytesEquals(byte[] a, byte[] b) {
		for (int index = 0; index < Math.min(a.length, b.length); index++) {
			if (a[index] != b[index]) {
				return false;
			}
		}
		return true;
	}

	public void emptyTemporaryBuffer() {
		while (temporaryPacketBuffer.size() > 0) {	
			postProcessPacket(temporaryPacketBuffer.remove());
		}
	}

	void postProcessPacket(RawPacket rawPacket) {
		var macSource = (Long)((rawPacket.bytes[11] & 0xFFL) | ((rawPacket.bytes[10] & 0xFFL) << 8) | ((rawPacket.bytes[9] & 0xFFL) << 16) | ((rawPacket.bytes[8] & 0xFFL) << 24) | ((rawPacket.bytes[7] & 0xFFL) << 32) | ((rawPacket.bytes[6] & 0xFFL) << 40));
		var ethernetType = ((rawPacket.bytes[12] & 0xFF) << 8) | (rawPacket.bytes[13] & 0xFF);

		if (ethernetType == 0x8947) {
			var geonetPacket = new GNPacket(HexaHelper.byteArrayToString(Arrays.copyOfRange(rawPacket.bytes, 14, rawPacket.bytes.length)));

			if (geonetPacket.payloadType == GNPacket.camType) {
				var camPacket = UperEncoder.decode(HexaHelper.hexStringToByteArray(geonetPacket.payload), CAM.class);
				
				var stationId = camPacket.header.stationID.value;
				var latitude = camPacket.cam.camParameters.basicContainer.referencePosition.latitude.value;
				var longitude = camPacket.cam.camParameters.basicContainer.referencePosition.longitude.value;

				macToStationIdMap.put(macSource, stationId);

				for (var reachedStation : rawPacket.reachedStations) {
					if (reachedStation.id == stationId) {
						packetProcessHandler.onPacketProcessed(new Packet(reachedStation.timestamp, stationId, latitude / 10000000.0, longitude / 10000000.0, rawPacket.reachedStations, PacketType.CAM, geonetPacket.ch.maximumHopLimit, geonetPacket.bh.routerHopLimit));
						break;
					}
				}
			} else if (geonetPacket.payloadType == GNPacket.denmType) {
				var gbcPacket = (GBC)geonetPacket.eh;
				
				var latitude = gbcPacket.SOPV.lat;
				var longitude = gbcPacket.SOPV.lng;

				if (geonetPacket.bh.routerHopLimit == 10) {
					var denmPacket = UperEncoder.decode(HexaHelper.hexStringToByteArray(geonetPacket.payload), DENM.class);
					
					var stationId = denmPacket.itsPduHeader.stationID.value;

					macToStationIdMap.put(macSource, stationId);

					for (var reachedStation : rawPacket.reachedStations) {
						if (reachedStation.id == stationId) {
							packetProcessHandler.onPacketProcessed(new Packet(reachedStation.timestamp, stationId, null, null, rawPacket.reachedStations, PacketType.DENM, geonetPacket.ch.maximumHopLimit, geonetPacket.bh.routerHopLimit));
							break;
						}
					}
				} else if (macToStationIdMap.containsKey(macSource)) {
					var stationId = macToStationIdMap.get(macSource);


					for (var reachedStation : rawPacket.reachedStations) {
						if (reachedStation.id == stationId) {
							packetProcessHandler.onPacketProcessed(new Packet(reachedStation.timestamp, stationId, null, null, rawPacket.reachedStations, PacketType.DENM, geonetPacket.ch.maximumHopLimit, geonetPacket.bh.routerHopLimit));
							break;
						}
					}
				} else {
					System.err.println("Cannot found the station informations");
				}
			} else {
				System.err.println("Cannot found the station informations");
			}
		}
	}

	class RawPacket {
		byte[] bytes;
		long initialTimestamp;
		ArrayList<ReachedStation> reachedStations;

		RawPacket(byte[] bytes, Long initialTimestamp, ArrayList<ReachedStation> reachedStations) {
			this.bytes = bytes;
			this.initialTimestamp = initialTimestamp;
			this.reachedStations = reachedStations;
		}
	}

	public interface ProcessedPacketHandler {
		public void processedPacketHandle(Packet packet, String packetRepr);
	}
}