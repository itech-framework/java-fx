package io.github.itech_framework.java_fx.ui.toast;

import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Configuration of a single toast notification.
 * <p>
 * Defaults for position, duration, offsets and size limits come from
 * {@link FxSettings}. All setters are fluent and return this instance, so a
 * configuration can be built in one expression:
 * 
 * <pre>{@code
 * ToastConfig config = ToastConfig.create().type(ToastType.SUCCESS).message("Saved").position(ToastPosition.TOP_RIGHT);
 * }</pre>
 */
public class ToastConfig {
	private String title;
	private String message;
	private ToastPosition position = FxSettings.get().toast().position();
	private Duration duration = Duration.millis(FxSettings.get().toast().durationMillis());
	private double offsetX = FxSettings.get().toast().offsetX();
	private double offsetY = FxSettings.get().toast().offsetY();
	private double width = 360;
	private double maxWidth = FxSettings.get().toast().maxWidth();
	private double maxHeight = FxSettings.get().toast().maxHeight();
	private ToastType type = ToastType.INFO;
	private boolean showIcon = true;
	private boolean showCloseButton = true;
	private boolean autoHide = true;
	private boolean showProgress;
	private String group;
	private Node content;
	private Runnable onDismiss;
	private Runnable onHidden;
	private String customStyleClass;
	private Color backgroundColor;
	private Color textColor;

	private boolean singleLineMessage;

	/**
	 * @return {@code true} if the message stays on one line and is cut with an
	 *         ellipsis
	 */
	public boolean isSingleLineMessage() {
		return singleLineMessage;
	}

	/**
	 * Keeps the message on a single line and cuts it with an ellipsis when it is
	 * too long. The full text is shown in a tooltip. Use it for text that changes
	 * often, such as progress status, so the toast height does not change.
	 *
	 * @param singleLineMessage {@code true} to keep the message on one line
	 * @return this configuration
	 */
	public ToastConfig singleLineMessage(boolean singleLineMessage) {
		this.singleLineMessage = singleLineMessage;
		return this;
	}

	/**
	 * @return the title text, or {@code null} to use the default title of the
	 *         {@link ToastType}
	 */
	public String getTitle() {
		return title;
	}

	/** @return the message text */
	public String getMessage() {
		return message;
	}

	/** @return the screen position of the toast */
	public ToastPosition getPosition() {
		return position;
	}

	/** @return how long the toast stays visible before auto-hiding */
	public Duration getDuration() {
		return duration;
	}

	/** @return the horizontal distance in pixels from the screen edge */
	public double getOffsetX() {
		return offsetX;
	}

	/** @return the vertical distance in pixels from the screen edge */
	public double getOffsetY() {
		return offsetY;
	}

	/**
	 * @return the preferred toast width in pixels, capped by {@link #getMaxWidth()}
	 */
	public double getWidth() {
		return width;
	}

	/** @return the type of the toast */
	public ToastType getType() {
		return type;
	}

	/** @return {@code true} if the type icon is displayed */
	public boolean isShowIcon() {
		return showIcon;
	}

	/**
	 * @return {@code true} if the close button is displayed in the top-right corner
	 */
	public boolean isShowCloseButton() {
		return showCloseButton;
	}

	/**
	 * @return {@code true} if the toast hides itself after {@link #getDuration()}
	 */
	public boolean isAutoHide() {
		return autoHide;
	}

	/** @return {@code true} if a progress bar is displayed below the message */
	public boolean isShowProgress() {
		return showProgress;
	}

	/**
	 * @return the stack group name, or {@code null} for the default stack of the
	 *         position
	 */
	public String getGroup() {
		return group;
	}

	/** @return the custom content node replacing the message, or {@code null} */
	public Node getContent() {
		return content;
	}

	/**
	 * @return the callback run when the user clicks the close button, or
	 *         {@code null}
	 */
	public Runnable getOnDismiss() {
		return onDismiss;
	}

	/**
	 * @return the callback run after the toast is hidden for any reason, or
	 *         {@code null}
	 */
	public Runnable getOnHidden() {
		return onHidden;
	}

	/** @return the upper limit of the toast width in pixels */
	public double getMaxWidth() {
		return maxWidth;
	}

	/** @return the upper limit of the toast height in pixels */
	public double getMaxHeight() {
		return maxHeight;
	}

	/**
	 * @return an extra CSS style class applied to the toast container, or
	 *         {@code null}
	 */
	public String getCustomStyleClass() {
		return customStyleClass;
	}

	/**
	 * @return the explicit background color, or {@code null} to use the stylesheet
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/** @return the explicit text color, or {@code null} to use the stylesheet */
	public Color getTextColor() {
		return textColor;
	}

	/**
	 * Sets the title.
	 *
	 * @param title the title text, or {@code null} to use the default title of the
	 *              type
	 * @return this configuration
	 */
	public ToastConfig title(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Sets the message. Long messages wrap onto multiple lines.
	 *
	 * @param message the message text
	 * @return this configuration
	 */
	public ToastConfig message(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Sets the screen position.
	 *
	 * @param position the position
	 * @return this configuration
	 */
	public ToastConfig position(ToastPosition position) {
		this.position = position;
		return this;
	}

	/**
	 * Sets how long the toast stays visible. Only used when auto-hide is enabled.
	 *
	 * @param duration the visible time
	 * @return this configuration
	 */
	public ToastConfig duration(Duration duration) {
		this.duration = duration;
		return this;
	}

	/**
	 * Sets the horizontal distance from the screen edge. For center positions it
	 * shifts the toast left or right.
	 *
	 * @param offsetX the offset in pixels
	 * @return this configuration
	 */
	public ToastConfig offsetX(double offsetX) {
		this.offsetX = offsetX;
		return this;
	}

	/**
	 * Sets the vertical distance from the screen edge. For middle positions it
	 * shifts the toast up or down.
	 *
	 * @param offsetY the offset in pixels
	 * @return this configuration
	 */
	public ToastConfig offsetY(double offsetY) {
		this.offsetY = offsetY;
		return this;
	}

	/**
	 * Sets the fixed width used by every toast so stacked toasts stay aligned. The
	 * effective width is the smaller of this value and {@link #getMaxWidth()}.
	 *
	 * @param width the width in pixels
	 * @return this configuration
	 */
	public ToastConfig width(double width) {
		this.width = width;
		return this;
	}

	/**
	 * Sets the toast type.
	 *
	 * @param type the type
	 * @return this configuration
	 */
	public ToastConfig type(ToastType type) {
		this.type = type;
		return this;
	}

	/**
	 * Shows or hides the type icon. The icon is never shown for
	 * {@link ToastType#CUSTOM}.
	 *
	 * @param showIcon {@code true} to show the icon
	 * @return this configuration
	 */
	public ToastConfig showIcon(boolean showIcon) {
		this.showIcon = showIcon;
		return this;
	}

	/**
	 * Shows or hides the close button floating in the top-right corner.
	 *
	 * @param showCloseButton {@code true} to show the close button
	 * @return this configuration
	 */
	public ToastConfig showCloseButton(boolean showCloseButton) {
		this.showCloseButton = showCloseButton;
		return this;
	}

	/**
	 * Enables or disables automatic hiding.
	 *
	 * @param autoHide {@code true} to hide the toast after the configured duration
	 * @return this configuration
	 */
	public ToastConfig autoHide(boolean autoHide) {
		this.autoHide = autoHide;
		return this;
	}

	/**
	 * Adds a progress bar below the message. It starts indeterminate and is driven
	 * by {@link Toast#updateProgress(double)}.
	 *
	 * @param showProgress {@code true} to show the progress bar
	 * @return this configuration
	 */
	public ToastConfig showProgress(boolean showProgress) {
		this.showProgress = showProgress;
		return this;
	}

	/**
	 * Sets the stack group. Toasts with the same position and group are stacked
	 * together, independently of toasts in other groups. Different groups at the
	 * same position are not placed relative to each other, so give each group its
	 * own position or offset.
	 *
	 * @param group the group name, or {@code null} for the default stack
	 * @return this configuration
	 */
	public ToastConfig group(String group) {
		this.group = group;
		return this;
	}

	/**
	 * Replaces the message with a custom node. If a content node is set and no
	 * title is given, the header (icon and title) is left out. Combine this with
	 * {@code showCloseButton(false)} when the content provides its own close
	 * control.
	 *
	 * @param content the content node
	 * @return this configuration
	 */
	public ToastConfig content(Node content) {
		this.content = content;
		return this;
	}

	/**
	 * Sets a callback run only when the user clicks the close button of the toast.
	 *
	 * @param onDismiss the callback
	 * @return this configuration
	 */
	public ToastConfig onDismiss(Runnable onDismiss) {
		this.onDismiss = onDismiss;
		return this;
	}

	/**
	 * Sets a callback run once after the toast is hidden, whatever the reason
	 * (auto-hide, close button or code).
	 *
	 * @param onHidden the callback
	 * @return this configuration
	 */
	public ToastConfig onHidden(Runnable onHidden) {
		this.onHidden = onHidden;
		return this;
	}

	/**
	 * Sets the upper limit of the toast width.
	 *
	 * @param maxWidth the maximum width in pixels
	 * @return this configuration
	 */
	public ToastConfig maxWidth(double maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * Sets the upper limit of the toast height.
	 *
	 * @param maxHeight the maximum height in pixels
	 * @return this configuration
	 */
	public ToastConfig maxHeight(double maxHeight) {
		this.maxHeight = maxHeight;
		return this;
	}

	/**
	 * Adds an extra CSS style class to the toast container for custom styling.
	 *
	 * @param customStyleClass the style class name
	 * @return this configuration
	 */
	public ToastConfig customStyleClass(String customStyleClass) {
		this.customStyleClass = customStyleClass;
		return this;
	}

	/**
	 * Overrides the background color of the stylesheet for this toast only.
	 *
	 * @param backgroundColor the background color
	 * @return this configuration
	 */
	public ToastConfig backgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	/**
	 * Overrides the text color of the stylesheet for this toast only.
	 *
	 * @param textColor the text color
	 * @return this configuration
	 */
	public ToastConfig textColor(Color textColor) {
		this.textColor = textColor;
		return this;
	}

	/**
	 * Creates a configuration with the default values from {@link FxSettings}.
	 *
	 * @return a new configuration
	 */
	public static ToastConfig create() {
		return new ToastConfig();
	}
}