import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.beans.property.BooleanProperty;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class MapVisualizer extends StackPane {
	JMapViewer jMapViewer;
	Coordinate centroid;
	boolean tracking;

	VBox settings;

	Map<MapMarkerDot, CheckBox> followSettings;

	Map<StationRepr, BooleanProperty> followSettingsTest;

	Thread cameraThread;

	MapVisualizer() {
		super();
		setMinWidth(0);

		jMapViewer = new JMapViewer();
		
		((OsmTileLoader)jMapViewer.getTileController().getTileLoader()).headers.put("User-Agent", "MyApplication/1.0 (Windows 10; JavaFX)");
		
		jMapViewer.setTileSource(new OsmTileSource.Mapnik());
		jMapViewer.setDisplayPosition(new Coordinate(49.2428553, 4.0517071), 13);

		jMapViewer.setMinimumSize(new java.awt.Dimension(0, 0));

		var swingNode = new SwingNode();
		swingNode.setContent(jMapViewer);

		settings = new VBox();
		settings.setAlignment(Pos.BOTTOM_LEFT);

		settings.addEventFilter(MouseEvent.ANY, event -> swingNode.fireEvent(event));
        settings.addEventFilter(ScrollEvent.ANY, event -> swingNode.fireEvent(event));

		getChildren().addAll(swingNode, settings);

		followSettings = new HashMap<>();
		followSettingsTest = new HashMap<>();

		centroid = new Coordinate(49.2428553, 4.0517071);
		tracking = true;

		cameraThread = new Thread(() -> {
			System.out.println("Map Visualizer Camera Thread Started");
            while (!Thread.currentThread().isInterrupted()) {
            	if (tracking) {
					var position = jMapViewer.getPosition();

					var dLat = Math.pow(centroid.getLat() - position.getLat(), 2);
					var dLon = Math.pow(centroid.getLon() - position.getLon(), 2);

					var distance = Math.sqrt(dLat + dLon);

					var direction = Math.atan2(centroid.getLat() - position.getLat(), centroid.getLon() - position.getLon());
						
					var newPosition = new Coordinate(
						position.getLat() + distance * Math.sin(direction) * 0.02 * (1 - Math.exp(-distance * 600)),
						position.getLon() + distance * Math.cos(direction) * 0.02 * (1 - Math.exp(-distance * 600))
					);

					jMapViewer.setDisplayPosition(newPosition, jMapViewer.getZoom());
				}

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                	Thread.currentThread().interrupt();
                }
            }
			System.out.println("Map Visualizer Camera Thread Stopped");
        });
        cameraThread.start();
	}

	public void stopMapVisualizer() {
		cameraThread.interrupt();
		try {
			cameraThread.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateUi(Map<Long, StationRepr> stationsRepr) {
		Platform.runLater(() -> settings.getChildren().clear());

		for (var entry : stationsRepr.entrySet()) {
			var setting = new HBox(4);
			setting.setMaxWidth(HBox.USE_PREF_SIZE);

			var settingLabel = new Label(String.format("%08X", entry.getKey()));
			setting.setBackground(new Background(new BackgroundFill(entry.getValue().color.deriveColor(0, 1, 1, 0.6), null, null)));
			
			var followSettingCheckBox = new CheckBox("🚩");
			followSettingCheckBox.setSelected(true);

			var traceSettingCheckBox = new CheckBox("⛕");
			traceSettingCheckBox.setSelected(false);

			setting.getChildren().addAll(settingLabel, followSettingCheckBox, traceSettingCheckBox);
			Platform.runLater(() -> settings.getChildren().add(setting));

			followSettingsTest.put(entry.getValue(), followSettingCheckBox.selectedProperty());

			jMapViewer.addMapMarker(entry.getValue().marker);

			var polygon = new MapPolygonImpl(new ArrayList<>(entry.getValue().coordinates));

			polygon.setBackColor(new java.awt.Color(0, 0, 0, 0));
			polygon.setColor(entry.getValue().awtColor);
			polygon.setStroke(new java.awt.BasicStroke(3.0f));
			polygon.setVisible(false);

			traceSettingCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
				if (newVal) {
					polygon.setVisible(true);
				} else {
					polygon.setVisible(false);
				}

				jMapViewer.repaint();
			});
			
			jMapViewer.addMapPolygon(polygon);
		}
	}

	public void update() {
		var newCentroid = new Coordinate(0, 0);
		var stationToFollow = 0;

		for (var entry : followSettingsTest.entrySet()) {
			if (entry.getValue().getValue() && entry.getKey().marker.isVisible()) {
				newCentroid.setLon(newCentroid.getLon() + entry.getKey().marker.getLon());
				newCentroid.setLat(newCentroid.getLat() + entry.getKey().marker.getLat());
				
				stationToFollow++;
			}
		}

		newCentroid.setLat(newCentroid.getLat() / stationToFollow);
		newCentroid.setLon(newCentroid.getLon() / stationToFollow);

		if (stationToFollow > 0) {
			tracking = true;
			centroid = newCentroid;
		} else {
			tracking = false;
		}
	}

	public void animatePacket(MapMarkerDot source, MapMarkerDot destination, PacketType packetType, Double lectureSpeed, Player player) {
		var packet = new MapMarkerDot(packetType == PacketType.CAM ? "CAM" : "DENM", new Coordinate(source.getLat(), source.getLon()));
		packet.setBackColor(source.getBackColor());
		packet.setColor(java.awt.Color.BLACK);
		jMapViewer.addMapMarker(packet);

		new Thread(() -> {
			while (Math.pow(packet.getLat() - destination.getLat(), 2) + Math.pow(packet.getLon() - destination.getLon(), 2) > 0.000000001
				&& !player.lectureCancelled) {
				var direction = Math.atan2(destination.getLon() - packet.getLon(), destination.getLat() - packet.getLat());

				packet.setLat(packet.getLat() + Math.cos(direction) * 0.0001 * (1.0 / lectureSpeed));
				packet.setLon(packet.getLon() + Math.sin(direction) * 0.0001 * (1.0 / lectureSpeed));

				jMapViewer.repaint();

				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			jMapViewer.removeMapMarker(packet);
		}).start();
	}

public void animatePackett(MapMarkerDot source, MapMarkerDot destination, String packetRepr, Player player, long durationInMillis) {
    var packet = new MapMarkerDot(packetRepr, new Coordinate(source.getLat(), source.getLon()));
    packet.setBackColor(source.getBackColor());
    packet.setColor(java.awt.Color.BLACK);
    jMapViewer.addMapMarker(packet);

    // Calculate the distance between the source and destination
    double distance = Math.sqrt(Math.pow(destination.getLat() - source.getLat(), 2) + Math.pow(destination.getLon() - source.getLon(), 2));

    // Define the total animation duration in milliseconds
    long totalDuration = durationInMillis;

    // Calculate the number of steps based on 60 frames per second (adjust if needed)
    long stepInterval = 1000l / 240l; // roughly 60 FPS
    long steps = totalDuration / stepInterval;

    // Calculate the movement step size
    double stepSizeLat = (destination.getLat() - source.getLat()) / steps;
    double stepSizeLon = (destination.getLon() - source.getLon()) / steps;

    new Thread(() -> {
        for (long i = 0; i < steps && !player.lectureCancelled; i++) {
            packet.setLat(packet.getLat() + stepSizeLat);
            packet.setLon(packet.getLon() + stepSizeLon);

            jMapViewer.repaint();

            try {
                Thread.sleep(stepInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        packet.setLat(destination.getLat());
        packet.setLon(destination.getLon());
        jMapViewer.repaint();
        jMapViewer.removeMapMarker(packet);
    }).start();
}

}