package io.github.itech_framework.java_fx.ui.toast;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import io.github.itech_framework.java_fx.utils.SVGUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Toast notification class that displays non-blocking messages
 */
public class Toast {
    private final Popup popup;
    private final StackPane rootPane;
    private final VBox contentBox;
    private final ToastConfig config;
    private final Window owner;
    
    private Timeline autoHideTimeline;
    private ParallelTransition showAnimation;
    private ParallelTransition hideAnimation;
    
    private static final Map<ToastPosition, List<Toast>> activeToasts = new ConcurrentHashMap<>();
    private static final Duration SHOW_ANIMATION_DURATION = Duration.millis(300);
    private static final Duration HIDE_ANIMATION_DURATION = Duration.millis(200);
    private static final double TOAST_SPACING = 10;
    
    // Store the actual width and height for positioning
    private double actualWidth = 0;
    private double actualHeight = 0;
    
    private Toast(Window owner, ToastConfig config) {
        this.owner = owner;
        this.config = config;
        
        // Create popup (non-modal)
        popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        
        // Create content
        contentBox = new VBox(8);
        contentBox.setPadding(new Insets(15));
        contentBox.setMaxWidth(config.getMaxWidth());
        contentBox.setMaxHeight(config.getMaxHeight());
        contentBox.getStyleClass().add("toast-container");
        
        // Add custom style class if specified
        if (config.getCustomStyleClass() != null) {
            contentBox.getStyleClass().add(config.getCustomStyleClass());
        }
        
        // Create header with title and close button
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(10);
        
        // Add icon if enabled
        if (config.isShowIcon() && config.getType() != ToastType.CUSTOM) {
            javafx.scene.Node icon = SVGUtil.getIcon(
                getIconPathForType(config.getType()),
                Color.web(config.getType().getColor()),
                20, 20
            );
            icon.getStyleClass().add("toast-icon");
            headerBox.getChildren().add(icon);
        }
        
        // Title label
        Label titleLabel = new Label(config.getTitle() != null ? 
            config.getTitle() : config.getType().getDefaultTitle());
        titleLabel.getStyleClass().add("toast-title");
        titleLabel.setStyle("-fx-text-fill: " + getTextColor() + ";");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        headerBox.getChildren().add(titleLabel);
        
        // Close button if enabled
        if (config.isShowCloseButton()) {
            Button closeButton = new Button("✕");
            closeButton.getStyleClass().add("toast-close-button");
            closeButton.setOnAction(e -> hide());
            headerBox.getChildren().add(closeButton);
        }
        
        // Message label
        Label messageLabel = new Label(config.getMessage());
        messageLabel.getStyleClass().add("toast-message");
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: " + getTextColor() + ";");
        
        // Set background color if specified
        if (config.getBackgroundColor() != null) {
            contentBox.setStyle("-fx-background-color: " + toHex(config.getBackgroundColor()) + ";");
        } else {
            contentBox.setStyle("-fx-background-color: " + getBackgroundColorForType() + ";");
        }
        
        contentBox.getChildren().addAll(headerBox, messageLabel);
        
        // Create root pane with shadow effect
        rootPane = new StackPane(contentBox);
        rootPane.setStyle("-fx-background-color: transparent;");
        setupRoundedShadowEffect();
        
        // Set popup content
        popup.getContent().add(rootPane);
        
        // Set up animations
        setupAnimations();
        
        // Add to active toasts
        addToActiveToasts();
    }
    
    private String getIconPathForType(ToastType type) {
        switch (type) {
            case INFO:
                return SVGUtil.INFO_ICON_PATH;
            case SUCCESS:
                return SVGUtil.SUCCESS_ICON_PATH;
            case WARNING:
                return SVGUtil.WARNING_ICON_PATH;
            case ERROR:
                return SVGUtil.ERROR_ICON_PATH;
            default:
                return SVGUtil.INFO_ICON_PATH;
        }
    }
    
    private String getBackgroundColorForType() {
        switch (config.getType()) {
            case INFO:
                return "#d1ecf1";
            case SUCCESS:
                return "#d4edda";
            case WARNING:
                return "#fff3cd";
            case ERROR:
                return "#f8d7da";
            case CUSTOM:
                return config.getBackgroundColor() != null ? 
                    toHex(config.getBackgroundColor()) : "#ffffff";
            default:
                return "#ffffff";
        }
    }
    
    private String getTextColor() {
        if (config.getTextColor() != null) {
            return toHex(config.getTextColor());
        }
        
        switch (config.getType()) {
            case INFO:
                return "#0c5460";
            case SUCCESS:
                return "#155724";
            case WARNING:
                return "#856404";
            case ERROR:
                return "#721c24";
            default:
                return "#212529";
        }
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    private void setupRoundedShadowEffect() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(rootPane.widthProperty());
        clip.heightProperty().bind(rootPane.heightProperty());
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        rootPane.setClip(clip);
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.gray(0, 0.15));
        shadow.setRadius(5);
        shadow.setSpread(0.05);
        rootPane.setEffect(shadow);
    }
    
    private void setupAnimations() {
        // Show animation
        FadeTransition fadeIn = new FadeTransition(SHOW_ANIMATION_DURATION, rootPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition translateIn = new TranslateTransition(SHOW_ANIMATION_DURATION, rootPane);
        if (config.getPosition().isTop()) {
            translateIn.setFromY(-20);
            translateIn.setToY(0);
        } else if (config.getPosition().isBottom()) {
            translateIn.setFromY(20);
            translateIn.setToY(0);
        } else {
            translateIn.setFromY(0);
            translateIn.setToY(0);
        }
        
        showAnimation = new ParallelTransition(fadeIn, translateIn);
        
        // Hide animation
        FadeTransition fadeOut = new FadeTransition(HIDE_ANIMATION_DURATION, rootPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        
        TranslateTransition translateOut = new TranslateTransition(HIDE_ANIMATION_DURATION, rootPane);
        if (config.getPosition().isTop()) {
            translateOut.setFromY(0);
            translateOut.setToY(-20);
        } else if (config.getPosition().isBottom()) {
            translateOut.setFromY(0);
            translateOut.setToY(20);
        }
        
        hideAnimation = new ParallelTransition(fadeOut, translateOut);
        hideAnimation.setOnFinished(e -> {
            removeFromActiveToasts();
            popup.hide();
        });
        
        // Auto-hide timeline
        if (config.isAutoHide()) {
            autoHideTimeline = new Timeline(new KeyFrame(
                config.getDuration(),
                e -> hide()
            ));
            autoHideTimeline.setCycleCount(1);
        }
    }
    
    private void addToActiveToasts() {
        activeToasts.computeIfAbsent(config.getPosition(), k -> new ArrayList<>()).add(this);
        adjustToastPositions();
    }
    
    private void removeFromActiveToasts() {
        List<Toast> toasts = activeToasts.get(config.getPosition());
        if (toasts != null) {
            toasts.remove(this);
            if (toasts.isEmpty()) {
                activeToasts.remove(config.getPosition());
            } else {
                adjustToastPositions();
            }
        }
    }
    
    private void adjustToastPositions() {
        List<Toast> toasts = activeToasts.get(config.getPosition());
        if (toasts == null) return;
        
        Screen screen = getCurrentScreen();
        if (screen == null) return;
        
        double screenWidth = screen.getVisualBounds().getWidth();
        double screenHeight = screen.getVisualBounds().getHeight();
        double screenX = screen.getVisualBounds().getMinX();
        double screenY = screen.getVisualBounds().getMinY();
        
        for (int i = 0; i < toasts.size(); i++) {
            Toast toast = toasts.get(i);
            double x = calculateXPosition(screenWidth, screenX, i);
            double y = calculateYPosition(screenHeight, screenY, i);
            
            Platform.runLater(() -> {
                toast.popup.setX(x);
                toast.popup.setY(y);
            });
        }
    }
    
    private Screen getCurrentScreen() {
        if (owner != null) {
            // Get the screen where the owner window is located
            for (Screen screen : Screen.getScreens()) {
                if (screen.getBounds().contains(owner.getX(), owner.getY())) {
                    return screen;
                }
            }
        }
        // Fall back to primary screen
        return Screen.getPrimary();
    }
    
    private double calculateXPosition(double screenWidth, double screenX, int index) {
        // Calculate actual width if not already calculated
        if (actualWidth == 0) {
            actualWidth = Math.min(config.getMaxWidth(), estimateWidth());
        }
        
        double spacing = TOAST_SPACING;
        
        switch (config.getPosition()) {
            case TOP_LEFT:
            case MIDDLE_LEFT:
            case BOTTOM_LEFT:
                return screenX + config.getOffsetX() + (index * spacing);
                
            case TOP_CENTER:
            case MIDDLE_CENTER:
            case BOTTOM_CENTER:
                return screenX + (screenWidth - actualWidth) / 2 + config.getOffsetX();
                
            case TOP_RIGHT:
            case MIDDLE_RIGHT:
            case BOTTOM_RIGHT:
                // Fixed: Use actualWidth instead of config.getMaxWidth()
                return screenX + screenWidth - actualWidth - config.getOffsetX() - (index * spacing);
                
            default:
                return screenX + config.getOffsetX();
        }
    }
    
    private double calculateYPosition(double screenHeight, double screenY, int index) {
        // Calculate actual height if not already calculated
        if (actualHeight == 0) {
            actualHeight = estimateHeight();
        }
        
        double spacing = TOAST_SPACING;
        
        switch (config.getPosition()) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                return screenY + config.getOffsetY() + (index * (actualHeight + spacing));
                
            case MIDDLE_LEFT:
            case MIDDLE_CENTER:
            case MIDDLE_RIGHT:
                return screenY + (screenHeight - actualHeight) / 2 + config.getOffsetY() + (index * spacing);
                
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                return screenY + screenHeight - actualHeight - config.getOffsetY() - (index * (actualHeight + spacing));
                
            default:
                return screenY + config.getOffsetY();
        }
    }
    
    private double estimateWidth() {
        // Estimate width based on content
        double baseWidth = 300; // Default width
        if (config.getTitle() != null) {
            baseWidth = Math.max(baseWidth, config.getTitle().length() * 8);
        }
        if (config.getMessage() != null) {
            baseWidth = Math.max(baseWidth, Math.min(config.getMessage().length() * 7, config.getMaxWidth()));
        }
        return Math.min(baseWidth, config.getMaxWidth());
    }
    
    private double estimateHeight() {
        // Estimate height based on content
        double baseHeight = 60; // Minimum height
        if (config.getMessage() != null) {
            int lines = (int) Math.ceil(config.getMessage().length() / 50.0);
            baseHeight += (lines - 1) * 20;
        }
        return Math.min(baseHeight, config.getMaxHeight());
    }
    
    /**
     * Show the toast notification
     */
    public void show() {
        Platform.runLater(() -> {
            // Calculate actual dimensions
            actualWidth = estimateWidth();
            actualHeight = estimateHeight();
            
            Screen screen = getCurrentScreen();
            if (screen == null) return;
            
            double screenWidth = screen.getVisualBounds().getWidth();
            double screenHeight = screen.getVisualBounds().getHeight();
            double screenX = screen.getVisualBounds().getMinX();
            double screenY = screen.getVisualBounds().getMinY();
            
            double initialX = calculateXPosition(screenWidth, screenX, 0);
            double initialY = calculateYPosition(screenHeight, screenY, 0);
            
            // Debug logging
            System.out.println("Toast position - X: " + initialX + ", Y: " + initialY + 
                             ", Screen width: " + screenWidth + ", Toast width: " + actualWidth +
                             ", Offset: " + config.getOffsetX());
            
            popup.show(owner, initialX, initialY);
            
            // Play show animation
            showAnimation.play();
            
            // Start auto-hide timer if enabled
            if (config.isAutoHide() && autoHideTimeline != null) {
                autoHideTimeline.play();
            }
        });
    }
    
    /**
     * Hide the toast notification
     */
    public void hide() {
        Platform.runLater(() -> {
            if (autoHideTimeline != null) {
                autoHideTimeline.stop();
            }
            hideAnimation.play();
        });
    }
    
    /**
     * Show the toast immediately without animation
     */
    public void showImmediately() {
        Platform.runLater(() -> {
            // Calculate actual dimensions
            actualWidth = estimateWidth();
            actualHeight = estimateHeight();
            
            Screen screen = getCurrentScreen();
            if (screen == null) return;
            
            double screenWidth = screen.getVisualBounds().getWidth();
            double screenHeight = screen.getVisualBounds().getHeight();
            double screenX = screen.getVisualBounds().getMinX();
            double screenY = screen.getVisualBounds().getMinY();
            
            double initialX = calculateXPosition(screenWidth, screenX, 0);
            double initialY = calculateYPosition(screenHeight, screenY, 0);
            
            popup.show(owner, initialX, initialY);
            rootPane.setOpacity(1);
            
            if (config.isAutoHide() && autoHideTimeline != null) {
                autoHideTimeline.play();
            }
        });
    }
    
    /**
     * Hide the toast immediately without animation
     */
    public void hideImmediately() {
        Platform.runLater(() -> {
            if (autoHideTimeline != null) {
                autoHideTimeline.stop();
            }
            removeFromActiveToasts();
            popup.hide();
        });
    }
    
    /**
     * Update the toast message
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> {
            Label messageLabel = (Label) contentBox.getChildren().get(1);
            messageLabel.setText(message);
            // Recalculate dimensions as they might have changed
            actualHeight = 0;
            adjustToastPositions(); // Recalculate positions as height might change
        });
    }
    
    /**
     * Update the toast title
     */
    public void updateTitle(String title) {
        Platform.runLater(() -> {
            HBox headerBox = (HBox) contentBox.getChildren().get(0);
            Label titleLabel = (Label) headerBox.getChildren().get(1);
            titleLabel.setText(title);
        });
    }
    
    /**
     * Change the toast position
     */
    public void changePosition(ToastPosition newPosition) {
        config.position(newPosition);
        removeFromActiveToasts();
        addToActiveToasts();
    }
    
    /**
     * Get the toast configuration
     */
    public ToastConfig getConfig() {
        return config;
    }
    
    /**
     * Create a toast builder
     */
    public static ToastBuilder builder() {
        return new ToastBuilder();
    }
    
    /**
     * Clear all active toasts
     */
    public static void clearAll() {
        Platform.runLater(() -> {
            activeToasts.values().forEach(toastList -> {
                for (Toast toast : new ArrayList<>(toastList)) {
                    toast.hideImmediately();
                }
            });
            activeToasts.clear();
        });
    }
    
    /**
     * Toast builder class
     */
    public static class ToastBuilder {
        private Window owner;
        private ToastConfig config = new ToastConfig();
        
        public ToastBuilder owner(Window owner) {
            this.owner = owner;
            return this;
        }
        
        public ToastBuilder owner(Stage owner) {
            this.owner = owner;
            return this;
        }
        
        public ToastBuilder title(String title) {
            config.title(title);
            return this;
        }
        
        public ToastBuilder message(String message) {
            config.message(message);
            return this;
        }
        
        public ToastBuilder position(ToastPosition position) {
            config.position(position);
            return this;
        }
        
        public ToastBuilder duration(Duration duration) {
            config.duration(duration);
            return this;
        }
        
        public ToastBuilder offsetX(double offsetX) {
            config.offsetX(offsetX);
            return this;
        }
        
        public ToastBuilder offsetY(double offsetY) {
            config.offsetY(offsetY);
            return this;
        }
        
        public ToastBuilder type(ToastType type) {
            config.type(type);
            return this;
        }
        
        public ToastBuilder showIcon(boolean showIcon) {
            config.showIcon(showIcon);
            return this;
        }
        
        public ToastBuilder showCloseButton(boolean showCloseButton) {
            config.showCloseButton(showCloseButton);
            return this;
        }
        
        public ToastBuilder autoHide(boolean autoHide) {
            config.autoHide(autoHide);
            return this;
        }
        
        public ToastBuilder maxWidth(double maxWidth) {
            config.maxWidth(maxWidth);
            return this;
        }
        
        public ToastBuilder maxHeight(double maxHeight) {
            config.maxHeight(maxHeight);
            return this;
        }
        
        public ToastBuilder customStyleClass(String customStyleClass) {
            config.customStyleClass(customStyleClass);
            return this;
        }
        
        public ToastBuilder backgroundColor(Color backgroundColor) {
            config.backgroundColor(backgroundColor);
            return this;
        }
        
        public ToastBuilder textColor(Color textColor) {
            config.textColor(textColor);
            return this;
        }
        
        public Toast build() {
            if (owner == null) {
                // Try to get the primary stage
                owner = javafx.stage.Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst()
                    .orElse(null);
            }
            return new Toast(owner, config);
        }
        
        public void show() {
            build().show();
        }
    }
}