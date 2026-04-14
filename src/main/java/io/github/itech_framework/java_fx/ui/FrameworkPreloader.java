package io.github.itech_framework.java_fx.ui;

import java.util.prefs.Preferences;

import io.github.itech_framework.core.utils.PropertiesLoader;
import io.github.itech_framework.java_fx.ITechJavaFxApplication;
import io.github.itech_framework.java_fx.router.config.RouterConfig;
import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FrameworkPreloader extends Preloader {
    private Stage stage;
    private ProgressBar progressBar;
    private Label messageLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);

        PropertiesLoader.load(new String[]{"application.properties"}, getClass());
        String appName = PropertiesLoader.getProperty("app.name", "Application");

        boolean darkMode = false;
        try {
            Class<?> mainClass = ITechJavaFxApplication.getMainClass();
            Preferences prefs = Preferences.userNodeForPackage(mainClass);
            darkMode = prefs.getBoolean(RouterConfig.getDarkModeKey(), false);
        } catch (Exception e) {
        }

        Label titleLabel = new Label(appName);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        messageLabel = new Label("Starting...");
        
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(15);
        progressBar.setMaxHeight(15);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        VBox root = new VBox(15, titleLabel, messageLabel, progressBar);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20;-fx-background-color: -fx-background;");

        Scene scene = new Scene(root, 400, 200);
        String cssUrl = ITechJavaFxApplication.getMainClass().getResource("/static/css/root.css").toExternalForm();
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl);
            scene.getRoot().getStyleClass().add("root");
            if (darkMode) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
        }

        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        if (messageLabel != null) {
            double p = pn.getProgress();
            if (p < 0.3) {
                messageLabel.setText("Initializing application...");
            } else if (p < 0.6) {
                messageLabel.setText("Injecting data...");
            } else if (p < 0.8) {
                messageLabel.setText("Preparing components...");
            } else if (p < 0.95) {
                messageLabel.setText("Registering services...");
            } else {
                messageLabel.setText("Finalizing...");
            }
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START) {
            if (stage != null) {
                stage.hide();
            }
        }
    }
}