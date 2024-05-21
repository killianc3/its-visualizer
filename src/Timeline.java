import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ScrollPane;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Priority;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Spinner;

import java.util.List;

public class Timeline extends HBox {
	List<Packet> lectureList;
	Player player;

	VBox headers;

	ScrollPane scrollPane;

	VBox boxWrapper;
	Group groupWrapper;

	StackPane stack;

	Pane cursorPane;
	Rectangle cursor;

	VBox timeline;

	static final Color CAM_COLOR = Color.DODGERBLUE;
	static final Color CAM_COLOR_RECEIVE = CAM_COLOR.deriveColor(0, 1, 1, 0.3);
	static final Color DENM_COLOR = Color.DEEPPINK;
	static final Color DENM_COLOR_RECEIVE = DENM_COLOR.deriveColor(0, 1, 1, 0.3);

	Timeline(List<Packet> lectureList, Player player) {
		super();

		this.lectureList = lectureList;
		this.player = player;

		headers = new VBox();

		scrollPane = new ScrollPane();

		boxWrapper = new VBox();
		groupWrapper = new Group();

		stack = new StackPane();

		cursorPane = new Pane();
		cursor = new Rectangle();

		timeline = new VBox();

		headers.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
		getChildren().add(headers);

		scrollPane.setContent(boxWrapper);
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		//scrollPane.setPannable(true);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		//HBox.setHgrow(scrollPane, Priority.ALWAYS);
		getChildren().add(scrollPane);

		groupWrapper.getChildren().add(stack);
		boxWrapper.getChildren().add(groupWrapper);
		boxWrapper.setOnScroll(event -> {
            if (event.isControlDown()) {
            	event.consume();

				var zoomFactor = Math.exp(event.getDeltaY() * 0.02);

				var viewportBounds = scrollPane.getViewportBounds();

				var valX = scrollPane.getHvalue() * (groupWrapper.getLayoutBounds().getWidth() - viewportBounds.getWidth());

            	stack.setScaleX(Math.max(stack.getScaleX() * zoomFactor, scrollPane.getWidth() / timeline.getWidth()));
            	scrollPane.layout();

				var posInZoomTarget = stack.parentToLocal(groupWrapper.parentToLocal(new Point2D(event.getX(), event.getY())));
				var adjustment = stack.getLocalToParentTransform().deltaTransform(posInZoomTarget.multiply(zoomFactor - 1));

				scrollPane.setHvalue((valX + adjustment.getX()) / (groupWrapper.getBoundsInLocal().getWidth() - viewportBounds.getWidth()));
            }
        });

		stack.getChildren().addAll(timeline, cursorPane);

		cursorPane.getChildren().add(cursor);
		cursorPane.setMouseTransparent(true);
		//cursorPane.prefHeightProperty().bind(timeline.heightProperty());

		cursor.setWidth(400);
		cursor.widthProperty().bind(Bindings.divide(2.0, stack.scaleXProperty()));
		cursor.heightProperty().bind(cursorPane.heightProperty());



		timeline.setOnMouseDragged(event -> {
			event.consume();
			cursor.setX(event.getX());
		});
		timeline.setOnMousePressed(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				event.consume();
				player.cancel();
			}
		});
		timeline.setOnMouseReleased(event -> {
			if (event.getButton() == MouseButton.PRIMARY) {
				event.consume();
				cursor.setX(event.getX());

				var posInTime = lectureList.get(0).emissionTimestamp + (event.getX() / timeline.getWidth()) * (lectureList.get(lectureList.size() - 1).emissionTimestamp - lectureList.get(0).emissionTimestamp);
				
				var posInPackets = 0;
				while (posInPackets < lectureList.size() && lectureList.get(posInPackets).emissionTimestamp < posInTime) {
					posInPackets++;
				}

				player.setLecturePosition(posInPackets);
				player.play();
			}
		});
	}

	public void addTimelineRow(TimelineRow timelineRow) {
		headers.getChildren().add(timelineRow.header);
		timeline.getChildren().add(timelineRow.canvas);
	}

	public void resetZoom() {
		if (timeline.getWidth() > 0) {
			stack.setScaleX(scrollPane.getWidth() / timeline.getWidth());
		}
	}

	public class TimelineRow {
		VBox header;

		Label title;
		HBox subTitle;
		CheckBox sendCheckBox;		
		CheckBox receiveCheckBox;		
		CheckBox followCheckBox;		
		CheckBox traceCheckBox;

		Spinner<Integer> offset;

		Pane canvas;

		TimelineRow(String repr, Color color) {
			header = new VBox(2);
			header.setAlignment(Pos.TOP_CENTER);

			title = new Label(repr);
			title.setBackground(new Background(new BackgroundFill(color, null, null)));

			subTitle = new HBox();
			subTitle.setAlignment(Pos.CENTER);

			sendCheckBox = new CheckBox("🔼");
			sendCheckBox.setSelected(true);

			sendCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
				for (var packetRectangle : canvas.getChildren()) {
					if (((Rectangle)packetRectangle).getFill().equals(CAM_COLOR)
						|| ((Rectangle)packetRectangle).getFill().equals(DENM_COLOR)) {
						((Rectangle)packetRectangle).setVisible(newValue);
					}
				}
			});

			receiveCheckBox = new CheckBox("🔽");
			receiveCheckBox.setSelected(true);

			receiveCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
				for (var packetRectangle : canvas.getChildren()) {
					if (((Rectangle)packetRectangle).getFill().equals(CAM_COLOR_RECEIVE)
						|| ((Rectangle)packetRectangle).getFill().equals(DENM_COLOR_RECEIVE)) {
						((Rectangle)packetRectangle).setVisible(newValue);
					}
				}
			});

			offset = new Spinner<Integer>(0, 100000, 0);
			offset.setPrefWidth(120);

			subTitle.getChildren().addAll(sendCheckBox, receiveCheckBox);

			header.getChildren().addAll(title, subTitle);

			canvas = new Pane();

			offset.valueProperty().addListener((javafx.beans.value.ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
				Platform.runLater(() -> {
					for (var child : canvas.getChildren()) {
						((Rectangle)child).setX(((Rectangle)child).getX() + newValue - oldValue);
					}
				});
			});
		}

		public Rectangle addPacket(long position, PacketType packetType, boolean isReception) {
			var packetRectangle = new PacketRepr(packetType, !isReception);
			packetRectangle.setX(position);
			packetRectangle.setWidth(2);
			
			switch (packetType) {
				case DENM:
					packetRectangle.setFill(!isReception ? DENM_COLOR : DENM_COLOR_RECEIVE);
					break;
				case CAM:
					packetRectangle.setFill(!isReception ? CAM_COLOR : CAM_COLOR_RECEIVE);
					break;
			}

			packetRectangle.widthProperty().bind(
				Bindings.when(packetRectangle.visibleProperty())
					.then(Bindings.max(20.0, Bindings.min(100.0, Bindings.divide(2.0, stack.scaleXProperty()))))
					.otherwise(0.0)
			);
			packetRectangle.heightProperty().bind(header.heightProperty());

			Platform.runLater(() -> canvas.getChildren().add(packetRectangle));

			return packetRectangle;
		}

		public class PacketRepr extends Rectangle {
			PacketType packetType;
			boolean emission;

			PacketRepr(PacketType packetType, boolean emission) {
				super();

				this.packetType = packetType;
				this.emission = emission;
			}
		}
	}
}