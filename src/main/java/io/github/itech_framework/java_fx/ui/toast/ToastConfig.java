package io.github.itech_framework.java_fx.ui.toast;

import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Configuration class for toast notifications
 */
public class ToastConfig {
    private String title;
    private String message;
    private ToastPosition position = ToastPosition.BOTTOM_RIGHT;
    private Duration duration = Duration.seconds(3);
    private double offsetX = 20;
    private double offsetY = 20;
    private ToastType type = ToastType.INFO;
    private boolean showIcon = true;
    private boolean showCloseButton = true;
    private boolean autoHide = true;
    private double maxWidth = 350;
    private double maxHeight = 200;
    private String customStyleClass;
    private Color backgroundColor;
    private Color textColor;
    
    // Getters and setters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public ToastPosition getPosition() { return position; }
    public Duration getDuration() { return duration; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public ToastType getType() { return type; }
    public boolean isShowIcon() { return showIcon; }
    public boolean isShowCloseButton() { return showCloseButton; }
    public boolean isAutoHide() { return autoHide; }
    public double getMaxWidth() { return maxWidth; }
    public double getMaxHeight() { return maxHeight; }
    public String getCustomStyleClass() { return customStyleClass; }
    public Color getBackgroundColor() { return backgroundColor; }
    public Color getTextColor() { return textColor; }
    
    // Builder pattern methods
    public ToastConfig title(String title) {
        this.title = title;
        return this;
    }
    
    public ToastConfig message(String message) {
        this.message = message;
        return this;
    }
    
    public ToastConfig position(ToastPosition position) {
        this.position = position;
        return this;
    }
    
    public ToastConfig duration(Duration duration) {
        this.duration = duration;
        return this;
    }
    
    public ToastConfig offsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }
    
    public ToastConfig offsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }
    
    public ToastConfig type(ToastType type) {
        this.type = type;
        return this;
    }
    
    public ToastConfig showIcon(boolean showIcon) {
        this.showIcon = showIcon;
        return this;
    }
    
    public ToastConfig showCloseButton(boolean showCloseButton) {
        this.showCloseButton = showCloseButton;
        return this;
    }
    
    public ToastConfig autoHide(boolean autoHide) {
        this.autoHide = autoHide;
        return this;
    }
    
    public ToastConfig maxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }
    
    public ToastConfig maxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }
    
    public ToastConfig customStyleClass(String customStyleClass) {
        this.customStyleClass = customStyleClass;
        return this;
    }
    
    public ToastConfig backgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }
    
    public ToastConfig textColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }
    
    public static ToastConfig create() {
        return new ToastConfig();
    }
}