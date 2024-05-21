import java.io.Serializable;

class SniffPacket implements Serializable {
	private static final long serialVersionUID = 1L;
	
	long captureTimestamp;
	long providerStationId;
	byte[] packetBytes;

	public SniffPacket(long captureTimestamp, long providerStationId, byte[] packetBytes) {
		this.captureTimestamp = captureTimestamp;
		this.providerStationId = providerStationId;
		this.packetBytes = packetBytes;
	}
}