import java.util.ArrayList;
import java.io.Serializable;

public class Packet implements Serializable {
	private static final long serialVersionUID = 1L;

	public long emissionTimestamp;
	public long emitterStationId;
	public Double emitterLatitude;
	public Double emitterLongitude;
	public PacketType packetType;
	public ArrayList<ReachedStation> reachedStations;

	public int maximumHopLimit;
	public int remainingHopLimit;

	Packet(long emissionTimestamp, long emitterStationId, Double emitterLatitude, Double emitterLongitude, ArrayList<ReachedStation> reachedStations, PacketType packetType, int maximumHopLimit, int remainingHopLimit) {
		this.emissionTimestamp = emissionTimestamp;
		this.emitterStationId = emitterStationId;
		this.emitterLatitude = emitterLatitude;
		this.emitterLongitude = emitterLongitude;
		this.reachedStations = reachedStations;
		this.packetType = packetType;

		this.maximumHopLimit = maximumHopLimit;
		this.remainingHopLimit = remainingHopLimit;
	}
}