package io.github.itech_framework.java_fx.ui.toast;

/**
 * Screen positions where a toast can be displayed.
 * <p>
 * Positions are relative to the visual bounds of the screen that contains the
 * owner window. Toasts that share a position are stacked on top of each other.
 * The vertical and horizontal helper methods ({@code isTop()}, {@code isLeft()}, ...)
 * all return {@code false} for {@link #CUSTOM}.
 */
public enum ToastPosition {
    /** Top edge, left corner. */
    TOP_LEFT,

    /** Top edge, horizontally centered. */
    TOP_CENTER,

    /** Top edge, right corner. */
    TOP_RIGHT,

    /** Vertically centered, left edge. */
    MIDDLE_LEFT,

    /** Centered both vertically and horizontally. */
    MIDDLE_CENTER,

    /** Vertically centered, right edge. */
    MIDDLE_RIGHT,

    /** Bottom edge, left corner. */
    BOTTOM_LEFT,

    /** Bottom edge, horizontally centered. */
    BOTTOM_CENTER,

    /** Bottom edge, right corner. */
    BOTTOM_RIGHT,

    /** Placed using only the configured offsets from the top-left of the screen. */
    CUSTOM;

    /**
     * Checks whether this position is on the top row.
     *
     * @return {@code true} for {@link #TOP_LEFT}, {@link #TOP_CENTER} and {@link #TOP_RIGHT}
     */
    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }

    /**
     * Checks whether this position is on the middle row.
     *
     * @return {@code true} for {@link #MIDDLE_LEFT}, {@link #MIDDLE_CENTER} and {@link #MIDDLE_RIGHT}
     */
    public boolean isMiddle() {
        return this == MIDDLE_LEFT || this == MIDDLE_CENTER || this == MIDDLE_RIGHT;
    }

    /**
     * Checks whether this position is on the bottom row.
     *
     * @return {@code true} for {@link #BOTTOM_LEFT}, {@link #BOTTOM_CENTER} and {@link #BOTTOM_RIGHT}
     */
    public boolean isBottom() {
        return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
    }

    /**
     * Checks whether this position is aligned to the left edge.
     *
     * @return {@code true} for {@link #TOP_LEFT}, {@link #MIDDLE_LEFT} and {@link #BOTTOM_LEFT}
     */
    public boolean isLeft() {
        return this == TOP_LEFT || this == MIDDLE_LEFT || this == BOTTOM_LEFT;
    }

    /**
     * Checks whether this position is horizontally centered.
     *
     * @return {@code true} for {@link #TOP_CENTER}, {@link #MIDDLE_CENTER} and {@link #BOTTOM_CENTER}
     */
    public boolean isCenter() {
        return this == TOP_CENTER || this == MIDDLE_CENTER || this == BOTTOM_CENTER;
    }

    /**
     * Checks whether this position is aligned to the right edge.
     *
     * @return {@code true} for {@link #TOP_RIGHT}, {@link #MIDDLE_RIGHT} and {@link #BOTTOM_RIGHT}
     */
    public boolean isRight() {
        return this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT;
    }
}