package io.github.itech_framework.java_fx.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SplashScreen {
	private final Stage stage;
	private final ProgressBar progressBar;
	private final Label appNameLabel;

	public SplashScreen(String appName, boolean darkMode) {
		stage = new Stage(StageStyle.UNDECORATED);

		appNameLabel = new Label(appName);
		appNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

		Label loadingLabel = new Label("Loading application...");
		progressBar = new ProgressBar();
		progressBar.setPrefWidth(300);

		VBox root = new VBox(15, appNameLabel, loadingLabel, progressBar);
		root.setAlignment(Pos.CENTER);
		root.setStyle("-fx-padding: 20;");

		Scene scene = new Scene(root, 400, 200);

		if (darkMode) {
			scene.getStylesheets().add(getClass().getResource("/static/css/root.css").toExternalForm());
			scene.getRoot().getStyleClass().add("root");
			scene.getRoot().getStyleClass().add("dark-mode");
		} else {
			scene.getStylesheets().add(getClass().getResource("/static/css/root.css").toExternalForm());
			scene.getRoot().getStyleClass().add("root");
		}

		stage.setScene(scene);
	}

	public void show() {
		stage.show();
	}

	public void updateProgress(double progress) {
		progressBar.setProgress(progress);
	}

	public void close() {
		stage.close();
	}
}