public interface PacketProcessHandler {
	void onPacketProcessed(Packet packet);
	void onProcessingDone();
}