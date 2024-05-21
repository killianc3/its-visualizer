import java.io.Serializable;

public class ReachedStation implements Serializable {
	private static final long serialVersionUID = 1L;

	public long id;
	public long timestamp;

	ReachedStation(Long id, Long timestamp) {
		this.id = id;
		this.timestamp = timestamp;
	}
}