import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.DatePicker;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import javafx.scene.layout.GridPane;

import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;
import org.pcap4j.core.PcapHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProcessCaptureFilesTask extends Thread {
	Stage parentStage;
	PacketHandler packetHandler;

	Long startTimestamp;
	Long endTimestamp;

	List<CaptureFile> capturesFiles;

	PacketProcessHandler packetProcessHandler;

	ProcessCaptureFilesTask(Stage parentStage, PacketProcessHandler packetProcessHandler) {
		this.parentStage = parentStage;
		this.packetProcessHandler = packetProcessHandler;

		this.capturesFiles = new ArrayList<>();

		packetHandler = new PacketHandler(packetProcessHandler);
	}

	@Override
	public void run() {
		System.out.println("Process Capture File Thread Started");

		var fileChooser = new FileChooser();
			
		var fileFilter = new FileChooser.ExtensionFilter("Pcap files (*.pcap, *.pcapng)", "*.pcap", "*.pcapng");
		fileChooser.getExtensionFilters().add(fileFilter);

		Platform.runLater(() -> {
			var captureFiles = fileChooser.showOpenMultipleDialog(parentStage);

			if (captureFiles != null) {
				var captureFilesSceneContent = new VBox(10);
				captureFilesSceneContent.setAlignment(Pos.CENTER);
				captureFilesSceneContent.setPadding(new javafx.geometry.Insets(10));

				var captureFilesScene = new Scene(captureFilesSceneContent);
				var captureFilesStage = new Stage();
				captureFilesStage.initOwner(parentStage);
				captureFilesStage.setScene(captureFilesScene);

				var textFields = new ArrayList<TextField>();

				for (var captureFile : captureFiles) {
					var row = new HBox(10);
					row.setAlignment(Pos.CENTER);

					var rowTextField = new TextField();
					rowTextField.setPromptText("Station id");
					textFields.add(rowTextField);

					row.getChildren().addAll(new Label(captureFile.getName()), rowTextField);

					captureFilesSceneContent.getChildren().add(row);
				}

				var confirmationButton = new Button("Confirm");
				confirmationButton.setOnAction(e -> {
					captureFilesStage.close();

					startTimestamp = null;
					endTimestamp = null;

					for (var file : captureFiles) {
						var buffer = new Buffer(0l, file);

						while (buffer.tryNext()) {
							if (startTimestamp == null || buffer.packetTimestamp < startTimestamp) {
								startTimestamp = buffer.packetTimestamp;
							}

							if (endTimestamp == null || buffer.packetTimestamp > endTimestamp) {
								endTimestamp = buffer.packetTimestamp;
							}
						}
					}

					var timeSelectionContent = new VBox(4);
					timeSelectionContent.setAlignment(Pos.CENTER);
					timeSelectionContent.setPadding(new javafx.geometry.Insets(10));

					var startTimestampTextField = new TextField(startTimestamp.toString());
					var endTimestampTextField = new TextField(endTimestamp.toString());

					var timeSelectionConfirmationButton = new Button("Confirm");

					var startDateTime = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startTimestamp), java.time.ZoneId.systemDefault());
					
					var startDatePicker = new DatePicker(startDateTime.toLocalDate());
					startDatePicker.setPrefWidth(128);

					var startHourSpinner = new Spinner<Integer>(0, 23, startDateTime.getHour());
					startHourSpinner.setPrefWidth(96);
					
					var startMinuteSpinner = new Spinner<Integer>(0, 59, startDateTime.getMinute());
					startMinuteSpinner.setPrefWidth(96);
					
					var startSecondSpinner = new Spinner<Integer>(0, 59, startDateTime.getSecond());
					startSecondSpinner.setPrefWidth(96);

					var startContent = new HBox(4);
					startContent.setAlignment(Pos.CENTER);
					startContent.getChildren().addAll(
						new Label("Date"),
						startDatePicker,
						new Label("Hour"),
						startHourSpinner,
						new Label("Minute"),
						startMinuteSpinner,
						new Label("Second"),
						startSecondSpinner
					);

					var endDateTime = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(endTimestamp), java.time.ZoneId.systemDefault());
					
					var endDatePicker = new DatePicker(endDateTime.toLocalDate());
					endDatePicker.setPrefWidth(128);

					var endHourSpinner = new Spinner<Integer>(0, 23, endDateTime.getHour());
					endHourSpinner.setPrefWidth(96);
					
					var endMinuteSpinner = new Spinner<Integer>(0, 59, endDateTime.getMinute());
					endMinuteSpinner.setPrefWidth(96);
					
					var endSecondSpinner = new Spinner<Integer>(0, 59, endDateTime.getSecond());
					endSecondSpinner.setPrefWidth(96);

					var endContent = new HBox(4);
					endContent.setAlignment(Pos.CENTER);
					endContent.getChildren().addAll(
						new Label("Date"),
						endDatePicker,
						new Label("Hour"),
						endHourSpinner,
						new Label("Minute"),
						endMinuteSpinner,
						new Label("Second"),
						endSecondSpinner
					);

					timeSelectionContent.getChildren().addAll(startContent, endContent, timeSelectionConfirmationButton);

					var timeSelectionScene = new Scene(timeSelectionContent);
					var timeSelectionStage = new Stage();
					timeSelectionStage.initOwner(parentStage);
					timeSelectionStage.setScene(timeSelectionScene);

					timeSelectionConfirmationButton.setOnAction(ee -> {
						timeSelectionStage.close();

						var selectedStartLocalDateTime = java.time.LocalDateTime.of(
							startDatePicker.getValue(),
							java.time.LocalTime.of(
								startHourSpinner.getValue(),
								startMinuteSpinner.getValue(),
								startSecondSpinner.getValue()
						));

						var selectedEndLocalDateTime = java.time.LocalDateTime.of(
							endDatePicker.getValue(),
							java.time.LocalTime.of(
								endHourSpinner.getValue(),
								endMinuteSpinner.getValue(),
								endSecondSpinner.getValue()
						));

						startTimestamp = selectedStartLocalDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
						endTimestamp = selectedEndLocalDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

						for (var index = 0; index < captureFiles.size(); index++) {
							capturesFiles.add(new CaptureFile(Long.parseLong(textFields.get(index).getText()), captureFiles.get(index)));
						}

						var processTask = new ProcessTask();
						processTask.setOnSucceeded(eee -> packetProcessHandler.onProcessingDone());
						
						synchronized (this) {
							notify();
						}
					});

					timeSelectionStage.show();
				});
				captureFilesSceneContent.getChildren().add(confirmationButton);

				captureFilesStage.show();
			} else {
				synchronized (this) {
					interrupt();
				}
			}
		});

		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		var buffers = new ArrayList<Buffer>();

		for (var captureFile : capturesFiles) {
			buffers.add(new Buffer(captureFile));
		}

		for (var buffer : buffers) {
			buffer.tryNext();
		}

		var counter = 0;
		while (buffers.size() > 0 && !Thread.currentThread().isInterrupted()) {
			Buffer nextBuffer = null;
			for (var buffer : buffers) {
				if (nextBuffer == null || buffer.packetTimestamp < nextBuffer.packetTimestamp) {
					nextBuffer = buffer;
				}
			}

			if (nextBuffer.packetTimestamp > startTimestamp
				&& nextBuffer.packetTimestamp < endTimestamp) {
				packetHandler.processPacket(nextBuffer.packetBytes, nextBuffer.packetTimestamp, nextBuffer.stationId);
			}

			if (!nextBuffer.tryNext()) {
				buffers.remove(nextBuffer);
			}
		}

		packetHandler.emptyTemporaryBuffer();

		System.out.println("Process Capture File Thread Stopped");
	}

	class CaptureFile {
		long providerStationId;
		File file;

		CaptureFile(long providerStationId, File file) {
			this.providerStationId = providerStationId;
			this.file = file;
		}
	}

	class ProcessTask extends Task<Void> {
		@Override
		protected Void call() {
			var buffers = new ArrayList<Buffer>();

			for (var captureFile : capturesFiles) {
				buffers.add(new Buffer(captureFile));
			}

			for (var buffer : buffers) {
				buffer.tryNext();
 			}

 			var counter = 0;
 			while (buffers.size() > 0) {
 				Buffer nextBuffer = null;
 				for (var buffer : buffers) {
 					if (nextBuffer == null || buffer.packetTimestamp < nextBuffer.packetTimestamp) {
 						nextBuffer = buffer;
 					}
 				}

 				if (nextBuffer.packetTimestamp > startTimestamp
 					&& nextBuffer.packetTimestamp < endTimestamp) {
 					packetHandler.processPacket(nextBuffer.packetBytes, nextBuffer.packetTimestamp, nextBuffer.stationId);
 				}

 				if (!nextBuffer.tryNext()) {
 					buffers.remove(nextBuffer);
 				}
 			}

 			packetHandler.emptyTemporaryBuffer();

			return null;
		}
	}

	class Buffer {
		Long stationId;
		
		Packet packet;
		PcapHandle pcapHandle;

		long packetTimestamp;
		byte[] packetBytes;

		Buffer(Long stationId, File captureFile) {
			this.stationId = stationId;
			
			try {
				pcapHandle = Pcaps.openOffline(captureFile.getAbsolutePath());
			} catch (Exception e) {}
		}

		Buffer(CaptureFile captureFile) {
			this.stationId = captureFile.providerStationId;

			try {
				pcapHandle = Pcaps.openOffline(captureFile.file.getAbsolutePath());
			} catch (Exception e) {}
		}

		public boolean tryNext() {
			try {
				packet = pcapHandle.getNextPacket();

				packetTimestamp = pcapHandle.getTimestamp().getTime();
				packetBytes = packet.getRawData();

				if (packet != null) {

					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
	}
}