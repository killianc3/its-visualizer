import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.Tab;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.ListChangeListener;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;

import java.io.File;

public class Application extends javafx.application.Application {
	Map<Tab, Visualizer> visualizers;

	@Override
	public void start(Stage startStage) {
		setUserAgentStylesheet("primer-light.css");

		visualizers = new HashMap<>();

		var tabPane = new TabPane();

		var addTab = new Tab("Create Tab");
		addTab.setClosable(false);
		tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
			if(newTab == addTab) {
				var visualizer = new Visualizer(startStage);

				var tabContent = new HBox(60);
				tabContent.setAlignment(Pos.CENTER);

				var fileCaptureContent = new VBox(20);
				fileCaptureContent.setAlignment(Pos.CENTER_LEFT);
				fileCaptureContent.prefWidthProperty().bind(tabContent.widthProperty().multiply(36.0 / 100.0));

				var fileCaptureTitle = new TextFlow(new Text("Visualize ITS Communication with Capture Files"));
				fileCaptureTitle.getStyleClass().add("title-3");

				var fileCaptureDescription = new TextFlow(new Text("The visualizer interprets packet captures from all stations, allowing you to rewind time and observe their communications."));

				var fileCaptureButton = new Button("Open Capture Files");

				fileCaptureContent.getChildren().addAll(fileCaptureTitle, fileCaptureDescription, fileCaptureButton);

				var realtimeCaptureContent = new VBox(20);
				realtimeCaptureContent.setAlignment(Pos.CENTER_LEFT);
				realtimeCaptureContent.prefWidthProperty().bind(tabContent.widthProperty().multiply(36.0 / 100.0));

				var realtimeCaptureTitle = new TextFlow(new Text("Visualize ITS Communication in Real-time Globally"));
				realtimeCaptureTitle.getStyleClass().add("title-3");

				var realtimeCaptureDescription = new TextFlow(new Text("The visualizer connects to a server that receives communication data from all stations, enabling you to view real-time communication worldwide."));

				var realtimeCaptureButton = new Button("Connect To The Server");

				realtimeCaptureContent.getChildren().addAll(realtimeCaptureTitle, realtimeCaptureDescription, realtimeCaptureButton);

				tabContent.getChildren().addAll(fileCaptureContent, realtimeCaptureContent);

				var tab = new Tab("New Tab");
				tab.setContent(tabContent);

				tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
				tabPane.getSelectionModel().select(tabPane.getTabs().size() - 2);

				visualizers.put(tab, visualizer);

				fileCaptureButton.setOnAction(e -> {
					visualizer.startProcessCaptureFile();
					tab.setContent(visualizer);
				});

				realtimeCaptureButton.setOnAction(e -> {
					visualizer.startRealtimeCapture();
					tab.setContent(visualizer);
				});
			}
		});

		tabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
			while (c.next()) {
				if (c.wasRemoved()) {
					for (Tab removedTab : c.getRemoved()) {
						if (visualizers.containsKey(removedTab)) {
							visualizers.get(removedTab).stopVisualizer();
							visualizers.remove(removedTab);
						}
					}
				}
			}
		});

		try {
		var cacheFiles = new File("cache").listFiles();
		if (cacheFiles != null) {
			for (var file : cacheFiles) {
				var visualizer = new Visualizer(startStage);
				Platform.runLater(() -> visualizer.startProcessCachedCapture(file));

				var tab = new Tab();
				tab.setContent(visualizer);
				tab.setText(file.getName().split("_")[0]);

				tabPane.getTabs().add(tab);
				visualizers.put(tab, visualizer);
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}

		tabPane.getTabs().addAll(addTab);

		var startScene = new Scene(tabPane);
		startStage.setScene(startScene);
		startStage.setMaximized(true);
		startStage.show();
	}

	public void stop() throws Exception {
		super.stop();

		var cacheFiles = new File("cache").listFiles();
		if (cacheFiles != null) {
			for (var file : cacheFiles) {
				file.delete();
			}
		}
		
		for (var visualizer : visualizers.entrySet()) {
			visualizer.getValue().stopVisualizer();
			
			if (visualizer.getValue().lectureList.size() > 0) {
				try (var oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream("cache/" + visualizer.getKey().getText() + "_" + visualizer.getKey()))) {
					oos.writeObject(visualizer.getValue().lectureList);
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}

		Platform.exit();
		System.exit(0);
	}
}