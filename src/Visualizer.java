import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.CheckBox;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Priority;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.binding.Bindings;

import org.openstreetmap.gui.jmapviewer.Coordinate;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Arrays;

public class Visualizer extends BorderPane implements PacketProcessHandler, Player.PlayerHandler {
	ArrayList<Packet> lectureList;
	Double lectureSpeed;

	List<MapVisualizer> mapVisualizers;
	HBox mapVisualizersColumns;
	VBox mapVisualizersRows;
	
	Player player;
	Timeline timeline;
	Map<Long, StationRepr> stationsRepr;
	List<Thread> processes;
	Stage stage;
	Map<Long, Long> offsets;

	Map<PacketType, BooleanProperty> filters;

	Visualizer(Stage stage) {
		super();

		lectureList = new ArrayList<>();
		lectureSpeed = 1D;

		mapVisualizers = new ArrayList<>();
		mapVisualizersRows = new VBox();
		mapVisualizersColumns = new HBox();
		VBox.setVgrow(mapVisualizersColumns, Priority.ALWAYS);
		VBox.setVgrow(mapVisualizersRows, Priority.ALWAYS);

		player = new Player(this);
		player.start();
		
		timeline = new Timeline(lectureList, player);
		timeline.setFillHeight(true);
		VBox.setVgrow(timeline, Priority.ALWAYS);
		setBottom(timeline);

		timeline.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

		stationsRepr = new HashMap<>();
		processes = new ArrayList<>();
		this.stage = stage;

		offsets = new HashMap<>();

		filters = new HashMap<>();

		var topControls = new HBox(10);
		topControls.setAlignment(Pos.CENTER);
		topControls.setPadding(new javafx.geometry.Insets(10));
		setTop(topControls);

		var adjustOffsetButton = new ToggleButton("Adjust Time");
		adjustOffsetButton.setOnAction(e -> {
			Platform.runLater(() -> {
				for (var node : topControls.getChildren()) {
					node.setDisable(adjustOffsetButton.isSelected());
				}
				adjustOffsetButton.setDisable(false);

				adjustOffsetButton.setText(adjustOffsetButton.isSelected() ? "Confirm" : "Adjust Time");

				timeline.cursorPane.setPrefHeight(0);

				for (var stationRepr : stationsRepr.values()) {
					if (!adjustOffsetButton.isSelected()) {
						stationRepr.timelineRow.subTitle.getChildren().clear();
						stationRepr.timelineRow.subTitle.getChildren().addAll(stationRepr.timelineRow.sendCheckBox, stationRepr.timelineRow.receiveCheckBox);						
					} else {
						stationRepr.timelineRow.subTitle.getChildren().clear();
						stationRepr.timelineRow.subTitle.getChildren().add(stationRepr.timelineRow.offset);
					}
				}

				if (!adjustOffsetButton.isSelected()) {
					for (var packet : lectureList) {
						if (stationsRepr.containsKey(packet.emitterStationId)) {
							packet.emissionTimestamp += -offsets.get(packet.emitterStationId) + stationsRepr.get(packet.emitterStationId).timelineRow.offset.getValue();
						}

						for (var reachedStation : packet.reachedStations) {
							if (stationsRepr.containsKey(reachedStation.id)) {
								reachedStation.timestamp += -offsets.get(reachedStation.id) + stationsRepr.get(reachedStation.id).timelineRow.offset.getValue();
							}
						}
					}

					for (var entry : stationsRepr.entrySet()) {
						offsets.put(entry.getKey(), (Long)(long)(int)entry.getValue().timelineRow.offset.getValue());
					}

					lectureList.sort((Packet a, Packet b) -> Long.compare(a.emissionTimestamp, b.emissionTimestamp));
					System.out.println("Adjustment Done");
				}
			});
		});

		var newMapVisualizerButton = new Button("New Map");
		newMapVisualizerButton.setOnAction(e -> {
			var mapVisualizer = new MapVisualizer();
			HBox.setHgrow(mapVisualizer, Priority.ALWAYS);
			VBox.setVgrow(mapVisualizer, Priority.ALWAYS);
			mapVisualizer.updateUi(stationsRepr);

			mapVisualizers.add(mapVisualizer);
			mapVisualizersColumns.getChildren().add(mapVisualizer);
		});

		var newMapRowButton = new Button("New Map Row");
		newMapRowButton.setOnAction(e -> {
			mapVisualizersColumns = new HBox();
			VBox.setVgrow(mapVisualizersColumns, Priority.ALWAYS);
			mapVisualizersRows.getChildren().add(mapVisualizersColumns);

			var mapVisualizer = new MapVisualizer();
			HBox.setHgrow(mapVisualizer, Priority.ALWAYS);
			VBox.setVgrow(mapVisualizer, Priority.ALWAYS);
			mapVisualizer.updateUi(stationsRepr);

			mapVisualizers.add(mapVisualizer);
			mapVisualizersColumns.getChildren().add(mapVisualizer);
		});

		var resetZoomButton = new Button("Reset timeline zoom");
		resetZoomButton.setOnAction(e -> timeline.resetZoom());

		var filterMenu = new MenuButton("Filter");

		for (var packetType : PacketType.values()) {
			var filterMenuItem = new CheckMenuItem(packetType.toString());
			filterMenuItem.setSelected(true);
			filterMenuItem.selectedProperty().addListener((obs, oldValue, newValue) -> {
				for (var stationRepr : stationsRepr.values()) {
					for (var rectangle : stationRepr.timelineRow.canvas.getChildren()) {
						if (((Timeline.TimelineRow.PacketRepr)rectangle).packetType.equals(packetType)) {
							rectangle.setVisible(newValue);
						}
					}
				}
			});

			filters.put(packetType, filterMenuItem.selectedProperty());

			filterMenu.getItems().add(filterMenuItem);
		}

		var playPauseButton = new ToggleButton("Play");
		player.lecturePaused.addListener((obs, wasSelected, isSelected) -> {
			playPauseButton.setSelected(!isSelected);
        });

		playPauseButton.setOnAction(e -> {
			if (player.lecturePaused.getValue()) {
				player.play();
			} else {
				player.pause();
			}
		});

		var previousButton = new Button("Previous");
		previousButton.setOnAction(e -> {
			if (!player.lecturePaused.getValue()) {
				player.cancel();
			}
			player.previous();
		});

		var nextButton = new Button("Next");
		nextButton.setOnAction(e -> {
			if (!player.lecturePaused.getValue()) {
				player.cancel();
			}
			player.next();
		});

		topControls.getChildren().addAll(adjustOffsetButton, newMapVisualizerButton, newMapRowButton, resetZoomButton, filterMenu, previousButton, playPauseButton, nextButton);

		var mapVisualizer = new MapVisualizer();
		HBox.setHgrow(mapVisualizer, Priority.ALWAYS);

		mapVisualizers.add(mapVisualizer);

		mapVisualizersColumns.getChildren().addAll(mapVisualizer);
		mapVisualizersRows.getChildren().add(mapVisualizersColumns);
		
		setCenter(mapVisualizersRows);
	}

	public void stopVisualizer() {
		for (var process : processes) {
			if (process.isAlive()) {
				synchronized (process) {
					process.interrupt();
				}

				try {
					process.join();
				} catch (Exception e) {
				}
			}
		}

		for (var mapVisualizer : mapVisualizers) {
			mapVisualizer.stopMapVisualizer();
		}

		player.stopPlayer();

		try {
			player.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPacketProcessed(Packet packet) {
		lectureList.add(packet);

		if (stationsRepr.containsKey(packet.emitterStationId)) {
			if (packet.emitterLatitude != null && packet.emitterLongitude != null) {
				stationsRepr.get(packet.emitterStationId).coordinates.add(new Coordinate(packet.emitterLatitude, packet.emitterLongitude));
			}
		}

		var packetReprs = new ArrayList<Rectangle>();

		for (var reachedStation : packet.reachedStations) {
			if (!stationsRepr.containsKey(reachedStation.id)) {
				var stationRepr = new StationRepr(reachedStation.id, timeline);
				stationsRepr.put(reachedStation.id, stationRepr);
				offsets.put(reachedStation.id, 0L);

				for (var mapVisualizer : mapVisualizers) {
					mapVisualizer.updateUi(stationsRepr);
				}

				Platform.runLater(() -> timeline.addTimelineRow(stationRepr.timelineRow));	
			}

			var stationRepr = stationsRepr.get(reachedStation.id);

			var elapsedTime = reachedStation.timestamp - lectureList.get(0).emissionTimestamp;

			packetReprs.add(stationRepr.timelineRow.addPacket(elapsedTime, packet.packetType, packet.emitterStationId != reachedStation.id));

			Platform.runLater(() -> {
				if (lectureList.size() % 10 == 0) {
					timeline.resetZoom();
 				}
 			});
		}

		for (var packetRepr : packetReprs) {
			packetRepr.setOnMouseEntered(event -> {
				for (var a : packetReprs) {
					a.setFill(((Color)a.getFill()).deriveColor(0, 1, 0.5, 1));
				}
			});

			packetRepr.setOnMouseExited(event -> {
				for (var a : packetReprs) {
					a.setFill(((Color)a.getFill()).deriveColor(0, 1, 2, 1));
				}
			});
		}
 	}

 	@Override
 	public void onProcessingDone() {
 		for (var stationRepr : stationsRepr.values()) {
			for (var mapVisualizer : mapVisualizers) {
				mapVisualizer.updateUi(stationsRepr);
			}
		}

 		Platform.runLater(() -> timeline.resetZoom());
 	}

 	@Override
 	public long play(int lecturePosition) {
 		if (lecturePosition < lectureList.size()) {
 			lecturePosition = Math.max(0, lecturePosition);
 			var packet = lectureList.get(lecturePosition);

 			if (stationsRepr.containsKey(packet.emitterStationId)) {
 				Platform.runLater(() -> {
 					timeline.cursor.setX(packet.emissionTimestamp - lectureList.get(0).emissionTimestamp);
 				});

	 			var stationRepr = stationsRepr.get(packet.emitterStationId);

	 			if (packet.emitterLatitude != null && packet.emitterLongitude != null) {
	 				stationRepr.marker.setLat(packet.emitterLatitude);
	 				stationRepr.marker.setLon(packet.emitterLongitude);
	 				stationRepr.marker.setVisible(true);
	 			}

	 			if (!filters.get(packet.packetType).getValue()) {
	 				return 0;
	 			}

	 			for (var mapVisualizer : mapVisualizers) {
	 				mapVisualizer.update();

	 				for (var reachedStation : packet.reachedStations) {
	 					if (stationsRepr.containsKey(reachedStation.id)
	 						&& reachedStation.id != packet.emitterStationId
	 						&& stationRepr.timelineRow.sendCheckBox.isSelected()
	 						&& stationsRepr.get(reachedStation.id).timelineRow.receiveCheckBox.isSelected()) {
	 						mapVisualizer.animatePackett(stationRepr.marker, stationsRepr.get(reachedStation.id).marker, packet.packetType.toString() + " " + packet.remainingHopLimit + "/" + packet.maximumHopLimit, player, 1000l);
	 					}
	 				}
	 			}

	 			if (packet.reachedStations.size() == 1) {
	 				return 0;
	 			} else if (lecturePosition < lectureList.size() - 1) {
	 				var a = 0l;
	 				for (var reachedStation : packet.reachedStations) {
	 					if (reachedStation.timestamp > a) {
	 						a = reachedStation.timestamp;
	 					}
	 				}
		 			return 2000;
 				} else {
 					return 0l;
 				}
	 		} else {
	 			return 0l;
	 		}
 		} else {
 			player.pause();
 			return 0l;
 		}
 	}

 	public void startProcessCaptureFile() {
 		var processCaptureFilesThread = new ProcessCaptureFilesTask(stage, this);
		processCaptureFilesThread.start();

		processes.add(processCaptureFilesThread);
 	}

 	public void startRealtimeCapture() {
 		var processRealtimeCaptureThread = new ProcessRealtimeCaptureTask(stage, this, offsets);
		processRealtimeCaptureThread.start();

		processes.add(processRealtimeCaptureThread);
 	}

	@SuppressWarnings("unchecked")
 	public void startProcessCachedCapture(File filePath) {
 		try (var ois = new java.io.ObjectInputStream(new java.io.FileInputStream(filePath))) {
            var cachedPackets = (ArrayList<Packet>) ois.readObject();

            if (cachedPackets != null) {
				var processCachedCaptureThread = new ProcessCachedCaptureTask(cachedPackets, this);
				processCachedCaptureThread.start();

				processes.add(processCachedCaptureThread);
			}
		} catch (java.io.IOException | ClassNotFoundException e) {
			e.printStackTrace();
			lectureList = new ArrayList<>();
		}
 	}
}