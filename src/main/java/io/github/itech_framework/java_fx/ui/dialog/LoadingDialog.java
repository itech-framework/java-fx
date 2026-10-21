package io.github.itech_framework.java_fx.ui.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.itech_framework.core.utils.DataStorageUtil;
import io.github.itech_framework.core.utils.validator.CommonValidator;
import io.github.itech_framework.java_fx.router.config.RouterConfig;
import io.github.itech_framework.java_fx.utils.FxResources;
import io.github.itech_framework.java_fx.utils.node.StyleUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoadingDialog {

    private final Stage dialogStage;
    private final StackPane rootPane;
    private final VBox container;
    private final ProgressIndicator progressIndicator;
    private final Label messageLabel;
    private final VBox contentBox;
    private Stage ownerStage;
    private Effect originalOwnerEffect;
    private Paint originalFill;
    
    private final AtomicBoolean isShowing = new AtomicBoolean(false);
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    
    private static final Duration ANIMATION_DURATION = Duration.millis(300);
    private static LoadingDialog currentLoadingDialog;
    
    private LoadingDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setResizable(false);
        
        // Create progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.getStyleClass().add("loading-progress");
        
        // Create message label
        messageLabel = new Label("Loading...");
        messageLabel.getStyleClass().add("loading-message");
        
        // Content layout
        contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(progressIndicator, messageLabel);
        
        // Main container
        container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(contentBox);
        container.getStyleClass().add("loading-container");
        
        // Root pane
        rootPane = new StackPane(container);
        rootPane.setPadding(new Insets(15));
        rootPane.setStyle("-fx-background-color: transparent;");
        
        // Configure rounded corners with shadow
        setupRoundedShadowEffect();
        
        // Scene setup
        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        FxResources.applyTheme(scene.getStylesheets());
        dialogStage.setScene(scene);
        
        if (owner != null) {
            ownerStage = owner;
            dialogStage.initOwner(owner);
            Scene ownerScene = owner.getScene();
            if (ownerScene != null) {
                Parent root = ownerScene.getRoot();
                originalOwnerEffect = root.getEffect();
                originalFill = ownerScene.getFill();
                applyBlurEffect(root);
            }
        }
        
        applyDarkMode();
        
        dialogStage.setOnShown(e -> {
            playShowAnimation();
            isShowing.set(true);
        });
        
        dialogStage.setOnHidden(e -> {
            isShowing.set(false);
            isClosing.set(false);
            if (owner != null) {
                Scene ownerScene = owner.getScene();
                if (ownerScene != null) {
                    Parent root = ownerScene.getRoot();
                    ownerScene.setFill(originalFill);
                    restoreOwnerEffect(root);
                }
            }
            currentLoadingDialog = null;
        });
        
        currentLoadingDialog = this;
    }
    
    private void setupRoundedShadowEffect() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rootPane.widthProperty());
        clip.heightProperty().bind(rootPane.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        rootPane.setClip(clip);
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.gray(0, 0.15));
        shadow.setRadius(10);
        shadow.setSpread(0.05);
        rootPane.setEffect(shadow);
    }
    
    private void applyDarkMode() {
        Object darkModeValue = DataStorageUtil.load(RouterConfig.getDarkModeKey());
        if (darkModeValue != null) {
            boolean isDarkMode = Boolean.parseBoolean(darkModeValue.toString());
            rootPane.getStyleClass().remove("dark-mode");
            if (ownerStage != null) {
                ownerStage.getScene().setFill(Color.WHITE);
            }
            if (isDarkMode) {
                rootPane.getStyleClass().add("dark-mode");
                if (ownerStage != null) {
                    ownerStage.getScene().setFill(Color.BLACK);
                }
            }
        }
    }
    
    private void applyBlurEffect(Node node) {
        GaussianBlur blur = new GaussianBlur(8);
        node.setEffect(blur);
    }
    
    private void restoreOwnerEffect(Node node) {
        node.setEffect(originalOwnerEffect);
    }
    
    private void playShowAnimation() {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1);
        scale.setToY(1);
        
        new ParallelTransition(fade, scale).play();
    }
    
    private void playHideAnimation(Runnable onFinished) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.9);
        scale.setToY(0.9);
        
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        transition.play();
    }
    
    /**
     * Update the loading message
     * @param message New message text
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }
    
    /**
     * Update the progress (use -1 for indeterminate)
     * @param progress Progress value between 0 and 1
     */
    public void updateProgress(double progress) {
        Platform.runLater(() -> {
            if (progress < 0) {
                progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            } else {
                progressIndicator.setProgress(progress);
            }
        });
    }
    
    /**
     * Show the loading dialog (non-blocking)
     */
    public void show() {
        Platform.runLater(() -> {
            dialogStage.sizeToScene();
            dialogStage.centerOnScreen();
            dialogStage.show();
        });
    }
    
    /**
     * Show the loading dialog and wait for it to be shown
     */
    public void showAndWait() {
        Platform.runLater(() -> {
            dialogStage.sizeToScene();
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
        });
    }
    
    /**
     * Close the loading dialog with animation
     */
    public void close() {
        if (isShowing.get() && !isClosing.getAndSet(true)) {
            Platform.runLater(() -> {
                if (dialogStage.isShowing()) {
                    playHideAnimation(() -> {
                        if (dialogStage.isShowing()) {
                            dialogStage.close();
                        }
                    });
                }
            });
        }
    }
    
    /**
     * Close the loading dialog immediately without animation
     */
    public void closeImmediately() {
        if (isShowing.get()) {
            Platform.runLater(() -> {
                if (dialogStage.isShowing()) {
                    dialogStage.close();
                }
            });
        }
    }
    
    /**
     * Check if the dialog is currently showing
     */
    public boolean isShowing() {
        return isShowing.get() && !isClosing.get();
    }
    
    public void applyFonts(List<String> fontFamilies) {
        if (CommonValidator.validList(fontFamilies)) {
            Node rootNode = dialogStage.getScene().getRoot();
            StyleUtils.setFontFamily(rootNode, fontFamilies);
        }
    }
    
    public static LoadingDialogBuilder builder() {
        return new LoadingDialogBuilder();
    }
    
    public static LoadingDialog getCurrentLoadingDialog() {
        return currentLoadingDialog;
    }
    
    public static class LoadingDialogBuilder {
        private Stage owner;
        private String message = "Loading...";
        private ProgressType progressType = ProgressType.INDETERMINATE;
        private final List<String> customFonts = new ArrayList<>();
        private boolean showProgressText = false;
        private double size = 60.0;
        
        public LoadingDialogBuilder owner(Stage owner) {
            this.owner = owner;
            return this;
        }
        
        public LoadingDialogBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public LoadingDialogBuilder progressType(ProgressType progressType) {
            this.progressType = progressType;
            return this;
        }
        
        public LoadingDialogBuilder customFonts(String... fontFamilies) {
            if (fontFamilies != null) {
                for (String font : fontFamilies) {
                    if (!customFonts.contains(font)) {
                        customFonts.add(font);
                    }
                }
            }
            return this;
        }
        
        public LoadingDialogBuilder showProgressText(boolean showProgressText) {
            this.showProgressText = showProgressText;
            return this;
        }
        
        public LoadingDialogBuilder size(double size) {
            this.size = size;
            return this;
        }
        
        public LoadingDialog build() {
            LoadingDialog dialog = new LoadingDialog(owner);
            
            // Set message
            dialog.messageLabel.setText(message);
            
            // Configure progress indicator based on type
            if (progressType == ProgressType.DETERMINATE) {
                dialog.progressIndicator.setProgress(0);
            }
            
            // Set size
            dialog.progressIndicator.setMaxSize(size, size);
            
            // Add progress text if requested
            if (showProgressText) {
                Label progressTextLabel = new Label("0%");
                progressTextLabel.getStyleClass().add("loading-progress-text");
                VBox.setMargin(progressTextLabel, new Insets(5, 0, 0, 0));
                dialog.container.getChildren().add(progressTextLabel);
            }
            
            // Apply custom fonts
            if (!customFonts.isEmpty()) {
                dialog.applyFonts(customFonts);
            }
            
            return dialog;
        }
    }
    
    public enum ProgressType {
        DETERMINATE,
        INDETERMINATE
    }
    
    /**
     * Show a simple loading dialog with default settings
     */
    public static LoadingDialog showLoading(Stage owner) {
        LoadingDialog dialog = builder()
                .owner(owner)
                .build();
        dialog.show();
        return dialog;
    }
    
    /**
     * Show a loading dialog with custom message
     */
    public static LoadingDialog showLoading(Stage owner, String message) {
        LoadingDialog dialog = builder()
                .owner(owner)
                .message(message)
                .build();
        dialog.show();
        return dialog;
    }
}