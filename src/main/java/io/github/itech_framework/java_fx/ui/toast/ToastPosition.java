package io.github.itech_framework.java_fx.ui.toast;

/**
 * Enum representing different positions for the toast notification
 */
public enum ToastPosition {
    // Top positions
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    
    // Middle positions
    MIDDLE_LEFT,
    MIDDLE_CENTER,
    MIDDLE_RIGHT,
    
    // Bottom positions
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    
    // Custom position (set by user)
    CUSTOM;
    
    /**
     * Check if position is at the top
     */
    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }
    
    /**
     * Check if position is in the middle
     */
    public boolean isMiddle() {
        return this == MIDDLE_LEFT || this == MIDDLE_CENTER || this == MIDDLE_RIGHT;
    }
    
    /**
     * Check if position is at the bottom
     */
    public boolean isBottom() {
        return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
    }
    
    /**
     * Check if position is on the left
     */
    public boolean isLeft() {
        return this == TOP_LEFT || this == MIDDLE_LEFT || this == BOTTOM_LEFT;
    }
    
    /**
     * Check if position is centered horizontally
     */
    public boolean isCenter() {
        return this == TOP_CENTER || this == MIDDLE_CENTER || this == BOTTOM_CENTER;
    }
    
    /**
     * Check if position is on the right
     */
    public boolean isRight() {
        return this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT;
    }
}