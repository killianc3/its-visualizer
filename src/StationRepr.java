import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;

import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import java.util.List;
import java.util.ArrayList;

public class StationRepr {
	static int counter = 0;
	public Color color;
	public java.awt.Color awtColor;

	public Timeline.TimelineRow timelineRow;

	public MapMarkerDot marker;
	public List<Coordinate> coordinates;
	public MapPolygonImpl polygon;

	StationRepr(long stationId, Timeline timeline) {
		color = Color.hsb(counter, 1.0, 1.0);

		marker = new MapMarkerDot(String.format("%08X", stationId), new Coordinate(0, 0));
		marker.setVisible(false);

		awtColor = java.awt.Color.getHSBColor(counter / 360.0f, 1f, 1f);
		marker.setBackColor(awtColor);
		marker.setColor(java.awt.Color.BLACK);

		timelineRow = timeline.new TimelineRow("Station " + String.format("%08X", stationId), color.deriveColor(0, 1, 1, 0.4));

		counter += 60;

		coordinates = new ArrayList<>();
		polygon = new MapPolygonImpl(coordinates);

		polygon.setBackColor(new java.awt.Color(0, 0, 0, 0));
		polygon.setColor(awtColor);
		polygon.setStroke(new java.awt.BasicStroke(3.0f));
	}
}