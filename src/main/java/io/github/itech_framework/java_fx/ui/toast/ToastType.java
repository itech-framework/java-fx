package io.github.itech_framework.java_fx.ui.toast;

/**
 * Defines the kind of a toast notification.
 * <p>
 * Each type carries an accent color (used for the icon) and a default title
 * that is shown when no explicit title is configured.
 * {@link #CUSTOM} has no accent color and no icon, so the caller is expected to
 * supply their own background and text colors.
 */
public enum ToastType {
    /** General information. */
    INFO("#17a2b8", "Info"),

    /** An operation completed successfully. */
    SUCCESS("#28a745", "Success"),

    /** Something needs attention but is not a failure. */
    WARNING("#ffc107", "Warning"),

    /** An operation failed. */
    ERROR("#dc3545", "Error"),

    /** Fully caller-styled toast with no accent color and no icon. */
    CUSTOM(null, "Custom");

    private final String color;
    private final String defaultTitle;

    ToastType(String color, String defaultTitle) {
        this.color = color;
        this.defaultTitle = defaultTitle;
    }

    /**
     * Returns the accent color of this type.
     *
     * @return a hex color string such as {@code "#28a745"}, or {@code null} for {@link #CUSTOM}
     */
    public String getColor() {
        return color;
    }

    /**
     * Returns the title displayed when the toast configuration does not specify one.
     *
     * @return the default title text, never {@code null}
     */
    public String getDefaultTitle() {
        return defaultTitle;
    }
}