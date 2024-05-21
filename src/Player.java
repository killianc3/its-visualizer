import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.List;

public class Player extends Thread {
	int lecturePosition;
	BooleanProperty lecturePaused;
	boolean lectureCancelled;

	PlayerHandler playerHandler;

	boolean stopFlag;

	public interface PlayerHandler {
		long play(int lecturePosition);
	}

	Player(PlayerHandler playerHandler) {
		lecturePosition = 0;
		
		lecturePaused = new SimpleBooleanProperty();
		lecturePaused.setValue(true);

		lectureCancelled = false;

		this.playerHandler = playerHandler;

		this.stopFlag = false;
	}

	public void pause() {
		synchronized (this) {
			lecturePaused.setValue(true);

			if (getState() != Thread.State.WAITING) {
				interrupt();
			}
		}

		while (getState() != Thread.State.WAITING) {}
	}

	public void cancel() {
		synchronized (this) {
			lectureCancelled = true;
			lecturePaused.setValue(true);
			lecturePosition--;

			if (getState() != Thread.State.WAITING) {
				interrupt();
			}
		}

		while (getState() != Thread.State.WAITING) {}
	}

	public synchronized void play() {
		lecturePaused.setValue(false);
		lectureCancelled = false;
		
		notify();
	}

	public void stopPlayer() {
		cancel();

		synchronized (this) {
			stopFlag = true;
			notify();
		}
	}

	public synchronized int getLecturePosition() {
		return lecturePosition;
	}

	public synchronized void setLecturePosition(int lecturePosition) {
		this.lecturePosition = lecturePosition;
	}

	public void next() {
		lectureCancelled = false;
		lecturePosition++;
		while (playerHandler.play(lecturePosition) == 0) {
			lecturePosition++;
		};
	}

	public void previous() {
		lectureCancelled = false;
		lecturePosition--;
		while (playerHandler.play(lecturePosition) == 0) {
			lecturePosition--;
		};
	}

	@Override
	public void run() {
		System.out.println("Player Thread Started");

		while (!stopFlag) {
			var timeToSleep = 0L;

			synchronized (this) {
				if (lecturePaused.getValue()) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					timeToSleep = playerHandler.play(lecturePosition);
					lecturePosition++;
				}
			}

			while (timeToSleep > 10L && !lecturePaused.getValue()) {
				var sleepAt = System.currentTimeMillis();
				
				try {
					sleep(timeToSleep);
					break;
				} catch (InterruptedException e) {
					if (lectureCancelled) {
						break;
					}

					timeToSleep -= System.currentTimeMillis() - sleepAt;

					synchronized (this) {
						try {
							wait();
						} catch (InterruptedException ee) {
							ee.printStackTrace();
						}
					}
				}
			}
		}

		System.out.println("Player Thread Stopped");
	}
}