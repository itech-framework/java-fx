package io.github.itech_framework.java_fx.ui;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.storage.DefaultDataStorageService;
import io.github.itech_framework.java_fx.ITechJavaFxApplication;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxResources;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class FrameworkPreloader extends Preloader {
    private static final Logger logger = LogManager.getLogger(FrameworkPreloader.class);
    private static final Duration ANIMATION_DURATION = Duration.millis(500);

    private FxSettings.Preloader settings;
    private Stage stage;
    private VBox root;
    private ProgressBar progressBar;
    private Label messageLabel;
    private Timeline animation;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FxSettings fx = FxSettings.get();
        settings = fx.preloader();
        stage = primaryStage;
        stage.initStyle(settings.decorated() ? StageStyle.DECORATED : StageStyle.UNDECORATED);
        stage.setTitle(fx.app().name());

        Label titleLabel = new Label(fx.app().name());
        titleLabel.setStyle("-fx-font-size: " + settings.titleFontSize() + "px; -fx-font-weight: bold;");
        messageLabel = new Label(settings.startingMessage());

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(settings.barWidth());
        progressBar.setPrefHeight(settings.barHeight());
        progressBar.setMaxHeight(settings.barHeight());
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        root = new VBox(15, titleLabel, messageLabel, progressBar);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20;-fx-background-color: -fx-background;");

        Scene scene = new Scene(root, settings.width(), settings.height());
        String stylesheet = resolveStylesheet(fx);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
            root.getStyleClass().add("root");
            if (isDarkMode(fx)) {
                root.getStyleClass().add(fx.theme().darkStyleClass());
            }
        }

        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (messageLabel == null) {
            return;
        }
        if (info instanceof ProgressNotification pn) {
            double progress = pn.getProgress();
            messageLabel.setText(messageFor(progress));
            if (progress > 0 && progress <= 1) {
                animateTo(progress);
            }
        }
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn) {
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        if (messageLabel == null) {
            return false;
        }
        String details = info.getDetails();
        if ((details == null || details.isBlank()) && info.getCause() != null) {
            details = info.getCause().getMessage();
        }
        messageLabel.setText(settings.failedMessage() + (details == null || details.isBlank() ? "" : ": " + details));
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #d32f2f;");
        if (animation != null) {
            animation.stop();
        }
        progressBar.setProgress(0);

        Button close = new Button("Close");
        close.setOnAction(e -> Platform.exit());
        root.getChildren().add(close);
        return true;
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START && stage != null) {
            if (animation != null && animation.getStatus() == Animation.Status.RUNNING) {
                animation.setOnFinished(e -> stage.hide());
            } else {
                stage.hide();
            }
        }
    }

    private void animateTo(double target) {
        if (animation != null) {
            animation.stop();
        }
        if (progressBar.getProgress() < 0) {
            progressBar.setProgress(0);
        }
        animation = new Timeline(new KeyFrame(
                ANIMATION_DURATION,
                new KeyValue(progressBar.progressProperty(), target, Interpolator.EASE_BOTH)));
        animation.play();
    }

    private String messageFor(double progress) {
        if (progress < 0.3) return settings.initializingMessage();
        if (progress < 0.6) return settings.injectingMessage();
        if (progress < 0.8) return settings.preparingMessage();
        if (progress < 0.95) return settings.registeringMessage();
        return settings.finalizingMessage();
    }

    private String resolveStylesheet(FxSettings fx) {
        String path = settings.stylesheet();
        if (path.isBlank() && fx.theme().enabled()) {
            path = fx.theme().stylesheet();
        }
        if (path.isBlank()) {
            return null;
        }
        URL url = FxResources.find(ITechJavaFxApplication.getMainClass(), path);
        if (url == null) {
            logger.warn("Preloader stylesheet not found: {} (continuing without it)", path);
            return null;
        }
        return url.toExternalForm();
    }

    private boolean isDarkMode(FxSettings fx) {
        return switch (settings.theme()) {
            case DARK -> true;
            case LIGHT -> false;
            case AUTO -> storedDarkMode(fx.theme().darkModeKey());
        };
    }

    private boolean storedDarkMode(String key) {
        try {
            String stored = new DefaultDataStorageService().load(key);
            return stored != null && Boolean.parseBoolean(stored.replace("\"", ""));
        } catch (RuntimeException e) {
            logger.debug("Could not read the stored dark mode value", e);
            return false;
        }
    }
}