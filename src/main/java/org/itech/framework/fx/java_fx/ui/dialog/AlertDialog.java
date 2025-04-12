package org.itech.framework.fx.java_fx.ui.dialog;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.itech.framework.fx.core.utils.DataStorageUtil;
import org.itech.framework.fx.java_fx.router.config.RouterConfig;
import org.itech.framework.fx.java_fx.utils.SVGUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlertDialog {
    public enum Level {
        ERROR("#dc3545", SVGUtil.ERROR_ICON_PATH, "Error"),
        WARNING("#ffc107", SVGUtil.WARNING_ICON_PATH, "Warning"),
        INFO("#17a2b8", SVGUtil.INFO_ICON_PATH, "Information"),
        SUCCESS("#28a745", SVGUtil.SUCCESS_ICON_PATH, "Success");

        final String color;
        final String svgPath;
        final String title;

        Level(String color, String svgPath, String defaultTitle) {
            this.color = color;
            this.svgPath = svgPath;
            this.title = defaultTitle;
        }
    }

    private final Stage dialogStage;
    private final VBox container;
    private final StackPane rootPane;
    private final HBox header;
    private final Label titleLabel;
    private final Label messageLabel;
    private final HBox buttonContainer;

    private static final Duration ANIMATION_DURATION = Duration.millis(300);

    private AlertDialog(Level level, String title, String message, Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        // Main content container
        container = new VBox(15);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("alert-container");

        // Create shadow wrapper
        rootPane = new StackPane(container);
        rootPane.setPadding(new Insets(10));
        rootPane.setStyle("-fx-background-color: transparent;");

        // Configure rounded corners with shadow
        setupRoundedShadowEffect();

        // Header setup
        header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon using SVGUtil
        Node icon = SVGUtil.getIcon(
                level.svgPath,
                Color.web(level.color),
                24,
                24
        );
        icon.getStyleClass().add("alert-icon");

        // Title
        titleLabel = new Label(title != null ? title : level.title);
        titleLabel.getStyleClass().add("alert-title");

        header.getChildren().addAll(icon, titleLabel);

        // Message
        messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);

        // Buttons
        buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        container.getChildren().addAll(header, messageLabel, buttonContainer);

        // Scene setup
        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/static/css/style.css")).toExternalForm()
        );
        dialogStage.setScene(scene);

        if (owner != null) {
            dialogStage.initOwner(owner);
        }

        applyDarkMode();
        dialogStage.setOnShown(e -> playShowAnimation());
    }

    private void playShowAnimation() {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(ANIMATION_DURATION, rootPane);
        translate.setFromY(20);
        translate.setToY(0);

        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(fade, translate, scale).play();
    }

    private void playHideAnimation(Runnable onFinished) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition translate = new TranslateTransition(ANIMATION_DURATION, rootPane);
        translate.setFromY(0);
        translate.setToY(20);

        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.95);
        scale.setToY(0.95);

        ParallelTransition transition = new ParallelTransition(fade, translate, scale);
        transition.setOnFinished(e -> onFinished.run());
        transition.play();
    }

    private void setupRoundedShadowEffect() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rootPane.widthProperty());
        clip.heightProperty().bind(rootPane.heightProperty());
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        rootPane.setClip(clip);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.gray(0, 0.2));
        shadow.setRadius(15);
        shadow.setSpread(0.1);
        rootPane.setEffect(shadow);
    }

    private void applyDarkMode() {
        Object darkModeValue = DataStorageUtil.load(RouterConfig.getDarkModeKey());
        if (darkModeValue != null) {
            boolean isDarkMode = Boolean.parseBoolean(darkModeValue.toString());
            rootPane.getStyleClass().remove("dark-mode");
            if (isDarkMode) {
                rootPane.getStyleClass().add("dark-mode");
            }
        }
    }

    public static AlertDialogBuilder builder() {
        return new AlertDialogBuilder();
    }

    public void show() {
        dialogStage.sizeToScene();
        dialogStage.centerOnScreen();
        dialogStage.showAndWait();
    }

    public void close() {
        playHideAnimation(dialogStage::close);
    }

    public static class AlertDialogBuilder {
        private Level level = Level.INFO;
        private String title;
        private String message;
        private final List<Button> buttons = new ArrayList<>();
        private Stage owner;
        private AlertDialog dialog;

        public AlertDialogBuilder level(Level level) {
            this.level = level;
            return this;
        }

        public AlertDialogBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AlertDialogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AlertDialogBuilder addButton(String text, Runnable action) {
            Button btn = new Button(text);
            btn.getStyleClass().add("alert-default-button");
            btn.setOnAction(e -> {
                if (action != null) action.run();
                closeDialog();
            });
            buttons.add(btn);
            return this;
        }

        public AlertDialogBuilder addButton(String text, String styleClass, Runnable action) {
            Button btn = new Button(text);
            btn.getStyleClass().add(styleClass);
            btn.setOnAction(e -> {
                if (action != null) action.run();
                closeDialog();
            });
            buttons.add(btn);
            return this;
        }

        public AlertDialogBuilder addOwner(Stage owner) {
            this.owner = owner;
            return this;
        }

        public AlertDialog build() {
            dialog = new AlertDialog(level, title, message, owner);

            if (buttons.isEmpty()) {
                dialog.buttonContainer.getChildren().add(createDefaultButton());
            } else {
                dialog.buttonContainer.getChildren().addAll(buttons);
            }

            return dialog;
        }

        private Button createDefaultButton() {
            Button okBtn = new Button("OK");
            okBtn.getStyleClass().add("alert-default-button");
            okBtn.setOnAction(e -> closeDialog());
            return okBtn;
        }

        private void closeDialog() {
            if (dialog != null) {
                dialog.close();
            }
        }
    }
}