package io.github.itech_framework.java_fx.ui.toast;

/**
 * Enum representing different types of toast notifications
 */
public enum ToastType {
    INFO("#17a2b8", "Info"),
    SUCCESS("#28a745", "Success"),
    WARNING("#ffc107", "Warning"),
    ERROR("#dc3545", "Error"),
    CUSTOM(null, "Custom");
    
    private final String color;
    private final String defaultTitle;
    
    ToastType(String color, String defaultTitle) {
        this.color = color;
        this.defaultTitle = defaultTitle;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getDefaultTitle() {
        return defaultTitle;
    }
}