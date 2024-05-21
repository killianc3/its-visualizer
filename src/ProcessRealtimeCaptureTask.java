import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.application.Platform;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.Socket;
import java.net.InetSocketAddress;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import java.util.Arrays;
import java.util.Map;

public class ProcessRealtimeCaptureTask extends Thread {
	PacketHandler packetHandler;
	Stage parentStage;
	Map<Long, Long> offsets;

	ProcessRealtimeCaptureTask(Stage parentStage, PacketProcessHandler packetProcessHandler, Map<Long, Long> offsets) {
		this.parentStage = parentStage;
		packetHandler = new PacketHandler(packetProcessHandler);
		this.offsets = offsets;
	}

	@Override
	public void run() {
		var tcpSocket = new Socket();
		ObjectInputStream ois = null;

		var serverInfoContent = new VBox(10);
		serverInfoContent.setAlignment(Pos.CENTER);
		serverInfoContent.setPadding(new javafx.geometry.Insets(10));

		var serverAddress = new TextField();
		serverAddress.setText("192.168.137.1");

		var serverPort = new TextField();
		serverPort.setText("9999");

		var serverInfoInputs = new HBox(10);
		serverInfoInputs.getChildren().addAll(new Label("Address IP"), serverAddress, new Label("Port"), serverPort);

		var confirmationButton = new Button("confirm");

		serverInfoContent.getChildren().addAll(serverInfoInputs, confirmationButton);


		Platform.runLater(() -> {
			var serverInfoScene = new Scene(serverInfoContent);
			var serverInfoStage = new Stage();
			serverInfoStage.initOwner(parentStage);
			serverInfoStage.setScene(serverInfoScene);

			serverInfoStage.show();

			confirmationButton.setOnAction(e -> {
				try {
					tcpSocket.connect(new InetSocketAddress(serverAddress.getText(), Integer.parseInt(serverPort.getText())));

					synchronized (this) {
						notify();
					}
					serverInfoStage.close();
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			});
		});

		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}

		try {
			ois = new ObjectInputStream(tcpSocket.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				var sniffPacket = (SniffPacket) ois.readObject();
				
				var offset = 0l;
				if (offsets.containsKey(sniffPacket.providerStationId)) {
					offset = offsets.get(sniffPacket.providerStationId);
				}

				packetHandler.processPacket(sniffPacket.packetBytes, sniffPacket.captureTimestamp + offset, sniffPacket.providerStationId);
			} catch (Exception e) {}
		}
	}
}