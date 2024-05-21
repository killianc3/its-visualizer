import java.util.List;

public class ProcessCachedCaptureTask extends Thread {
	List<Packet> cachedPackets;
	PacketProcessHandler packetProcessHandler;

	ProcessCachedCaptureTask(List<Packet> cachedPackets, PacketProcessHandler packetProcessHandler) {
		this.cachedPackets = cachedPackets;
		this.packetProcessHandler = packetProcessHandler;
	}

	@Override
	public void run() {
		for (var cachedPacket : cachedPackets) {
			packetProcessHandler.onPacketProcessed(cachedPacket);

			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		}
		packetProcessHandler.onProcessingDone();
	}
}