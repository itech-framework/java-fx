package io.github.itech_framework.java_fx.ui.toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import io.github.itech_framework.java_fx.utils.SVGUtil;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * A non-blocking toast notification shown in a popup on top of the owner window.
 * <p>
 * Every toast has a fixed width, so toasts stacked at the same position line up
 * on one edge. The height is measured from the real content and re-measured
 * whenever it changes, and stacked toasts are spaced using those measured heights.
 * The close button floats in the top-right corner of the toast.
 * <p>
 * Besides a title and message, a toast can show a progress bar
 * ({@link ToastConfig#showProgress(boolean)}, {@link #updateProgress(double)}) or any
 * custom node ({@link ToastConfig#content(Node)}). Toasts can be placed in a named stack
 * ({@link ToastConfig#group(String)}) so long-running toasts such as transfers do not
 * move ordinary notifications.
 * <p>
 * All visual design comes from the framework stylesheet (modern-styles.css), so applications
 * can restyle toasts by overriding its rules. The style classes are:
 * <ul>
 *   <li>{@code toast-root}: transparent outer wrapper that holds the shadow</li>
 *   <li>{@code toast-container}: the toast box, plus {@code toast-info}, {@code toast-success},
 *       {@code toast-warning}, {@code toast-error} or {@code toast-custom} for the type, and any
 *       class set through {@link ToastConfig#customStyleClass(String)}</li>
 *   <li>{@code toast-body}, {@code toast-header}, {@code toast-icon}, {@code toast-title},
 *       {@code toast-message}, {@code toast-progress-bar}, {@code toast-close-button}</li>
 * </ul>
 * The owner scene's stylesheets and its {@code dark-mode} class are mirrored onto the toast,
 * so toasts follow the application theme. Only an explicit {@code backgroundColor} or
 * {@code textColor} in the configuration is applied inline, as a per-toast override.
 * <p>
 * Toasts are created through {@link #builder()}:
 * <pre>{@code
 * Toast.builder()
 *     .type(ToastType.SUCCESS)
 *     .message("Saved successfully")
 *     .position(ToastPosition.TOP_RIGHT)
 *     .show();
 * }</pre>
 * All public methods may be called from any thread; the work is moved to the
 * JavaFX application thread.
 */
public class Toast {
    private static final String STYLE_ROOT = "toast-root";
    private static final String STYLE_CONTAINER = "toast-container";
    private static final String STYLE_BODY = "toast-body";
    private static final String STYLE_HEADER = "toast-header";
    private static final String STYLE_ICON = "toast-icon";
    private static final String STYLE_TITLE = "toast-title";
    private static final String STYLE_MESSAGE = "toast-message";
    private static final String STYLE_PROGRESS = "toast-progress-bar";
    private static final String STYLE_CLOSE_BUTTON = "toast-close-button";
    private static final String STYLE_THEME_ROOT = "root";
    private static final String STYLE_DARK = "dark-mode";
    private static final String STYLE_CLOSE_ICON = "toast-close-icon";

    private final Popup popup;
    private final StackPane rootPane;
    private final StackPane contentBox;
    private final Label titleLabel;
    private final Label messageLabel;
    private final ProgressBar progressBar;
    private final ToastConfig config;
    private final Window owner;

    private Timeline autoHideTimeline;
    private ParallelTransition showAnimation;
    private ParallelTransition hideAnimation;

    private Scene themeScene;
    private Parent themeRoot;
    private ListChangeListener<String> themeListener;
    private String registeredKey;
    private boolean hiddenNotified;

    private static final Map<String, List<Toast>> activeToasts = new ConcurrentHashMap<>();
    private static final Duration SHOW_ANIMATION_DURATION = Duration.millis(300);
    private static final Duration HIDE_ANIMATION_DURATION = Duration.millis(200);
    private static final double TOAST_SPACING = 10;

    private double actualWidth;
    private double actualHeight;

    /**
     * Builds the toast content and registers the toast in the stack of its position and group.
     * The toast is not visible until {@link #show()} is called.
     *
     * @param owner  the window the toast belongs to, may be {@code null} if no window is showing
     * @param config the toast configuration
     */
    private Toast(Window owner, ToastConfig config) {
        this.owner = owner;
        this.config = config;
        this.actualWidth = Math.min(config.getWidth(), config.getMaxWidth());

        popup = new Popup();
        popup.setAutoFix(false);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.widthProperty().addListener((observable, previous, current) -> onPopupResized());
        popup.heightProperty().addListener((observable, previous, current) -> onPopupResized());

        boolean hasContent = config.getContent() != null;
        boolean hasHeader = !hasContent || config.getTitle() != null;

        titleLabel = new Label(config.getTitle() != null ?
            config.getTitle() : config.getType().getDefaultTitle());
        titleLabel.getStyleClass().add(STYLE_TITLE);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        messageLabel = new Label(config.getMessage());
        messageLabel.getStyleClass().add(STYLE_MESSAGE);
        messageLabel.setWrapText(!config.isSingleLineMessage());
        messageLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        if (config.isSingleLineMessage()) {
            Tooltip fullText = new Tooltip();
            fullText.textProperty().bind(messageLabel.textProperty());
            messageLabel.setTooltip(fullText);
        }

        progressBar = config.isShowProgress() ? createProgressBar() : null;

        VBox body = new VBox();
        body.getStyleClass().add(STYLE_BODY);

        if (hasHeader) {
            body.getChildren().add(createHeader());
        }
        body.getChildren().add(hasContent ? config.getContent() : messageLabel);
        if (progressBar != null) {
            body.getChildren().add(progressBar);
        }
        body.heightProperty().addListener((observable, previous, current) -> {
            if (popup.isShowing()) {
                Platform.runLater(this::refresh);
            }
        });

        contentBox = new StackPane(body);
        contentBox.setMinWidth(actualWidth);
        contentBox.setPrefWidth(actualWidth);
        contentBox.setMaxWidth(actualWidth);
        contentBox.setMaxHeight(config.getMaxHeight());
        contentBox.getStyleClass().addAll(STYLE_CONTAINER, typeStyleClass(config.getType()));
        if (config.getCustomStyleClass() != null) {
            contentBox.getStyleClass().add(config.getCustomStyleClass());
        }

        if (config.getBackgroundColor() != null) {
            contentBox.setStyle("-fx-background-color: " + toHex(config.getBackgroundColor()) + ";");
        }
        if (config.getTextColor() != null) {
            String textStyle = "-fx-text-fill: " + toHex(config.getTextColor()) + ";";
            titleLabel.setStyle(textStyle);
            messageLabel.setStyle(textStyle);
        }

        if (config.isShowCloseButton()) {
            Region closeButton = createCloseButton();
            StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
            StackPane.setMargin(closeButton, new Insets(6, 6, 0, 0));
            contentBox.getChildren().add(closeButton);
        }

        rootPane = new StackPane(contentBox);
        rootPane.getStyleClass().addAll(STYLE_ROOT, STYLE_THEME_ROOT);

        popup.getContent().add(rootPane);
        bindOwnerTheme();
        setupAnimations();
        addToActiveToasts();
    }

    /**
     * Creates the header row with the optional type icon and the title.
     * The right padding keeps long titles clear of the floating close button.
     *
     * @return the header row
     */
    private HBox createHeader() {
        HBox headerBox = new HBox();
        headerBox.getStyleClass().add(STYLE_HEADER);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        if (config.isShowCloseButton()) {
            headerBox.setPadding(new Insets(0, 24, 0, 0));
        }

        if (config.isShowIcon() && config.getType() != ToastType.CUSTOM) {
            Node icon = SVGUtil.getIcon(
                getIconPathForType(config.getType()),
                Color.web(config.getType().getColor()),
                20, 20
            );
            icon.getStyleClass().add(STYLE_ICON);
            headerBox.getChildren().add(icon);
        }

        headerBox.getChildren().add(titleLabel);
        return headerBox;
    }

    /**
     * Creates the progress bar, initially indeterminate.
     *
     * @return the progress bar
     */
    private ProgressBar createProgressBar() {
        ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        bar.getStyleClass().add(STYLE_PROGRESS);
        bar.setMaxWidth(Double.MAX_VALUE);
        return bar;
    }

    /**
     * Builds the style class name for a toast type, for example {@code toast-success}.
     *
     * @param type the toast type
     * @return the style class name
     */
    private static String typeStyleClass(ToastType type) {
        return "toast-" + type.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Builds the key of the stack this toast belongs to, from its position and group.
     *
     * @return the stack key
     */
    private String stackKey() {
        return config.getPosition().name() + "|" + (config.getGroup() == null ? "" : config.getGroup());
    }

    /**
     * Creates the close control that floats in the top-right corner.
     * It is a plain pane with a drawn X, so theme rules for buttons and missing font glyphs cannot affect it.
     * The gray stroke set here is only a fallback; the {@code toast-close-icon} style class overrides it.
     *
     * @return the close control, wired to {@link #dismiss()}
     */
    private Region createCloseButton() {
        SVGPath icon = new SVGPath();
        icon.setContent("M0 0 L8 8 M8 0 L0 8");
        icon.setFill(Color.TRANSPARENT);
        icon.setStroke(Color.GRAY);
        icon.setStrokeWidth(1.5);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setMouseTransparent(true);
        icon.getStyleClass().add(STYLE_CLOSE_ICON);

        StackPane button = new StackPane(icon);
        button.getStyleClass().add(STYLE_CLOSE_BUTTON);
        button.setMinSize(20, 20);
        button.setPrefSize(20, 20);
        button.setMaxSize(20, 20);
        button.setCursor(Cursor.HAND);
        button.setFocusTraversable(false);
        button.setOnMouseClicked(e -> {
            e.consume();
            dismiss();
        });
        return button;
    }

    /**
     * Handles a click on the close button: runs the dismiss callback, then hides the toast.
     */
    private void dismiss() {
        Runnable callback = config.getOnDismiss();
        if (callback != null) {
            try {
                callback.run();
            } finally {
                hide();
            }
        } else {
            hide();
        }
    }

    /**
     * Mirrors the stylesheets and the {@code dark-mode} class of the owner scene onto this toast,
     * so the toast follows the application theme even though it lives in its own popup window.
     */
    private void bindOwnerTheme() {
        Scene scene = owner != null ? owner.getScene() : null;
        if (scene == null) {
            return;
        }
        themeScene = scene;
        themeRoot = scene.getRoot();
        Bindings.bindContent(rootPane.getStylesheets(), scene.getStylesheets());
        themeListener = change -> syncDarkMode();
        themeRoot.getStyleClass().addListener(themeListener);
        syncDarkMode();
    }

    /**
     * Copies the {@code dark-mode} class state of the owner root onto the toast root.
     */
    private void syncDarkMode() {
        boolean dark = themeRoot.getStyleClass().contains(STYLE_DARK);
        if (dark && !rootPane.getStyleClass().contains(STYLE_DARK)) {
            rootPane.getStyleClass().add(STYLE_DARK);
        } else if (!dark) {
            rootPane.getStyleClass().remove(STYLE_DARK);
        }
    }

    /**
     * Stops mirroring the owner theme. Called when the toast is hidden.
     */
    private void unbindOwnerTheme() {
        if (themeScene == null) {
            return;
        }
        Bindings.unbindContent(rootPane.getStylesheets(), themeScene.getStylesheets());
        themeRoot.getStyleClass().removeListener(themeListener);
        themeScene = null;
        themeRoot = null;
        themeListener = null;
    }

    /**
     * Maps a toast type to its SVG icon path.
     *
     * @param type the toast type
     * @return the SVG path, falling back to the info icon
     */
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

    /**
     * Converts a JavaFX color to a CSS hex string, ignoring opacity.
     *
     * @param color the color
     * @return a string such as {@code "#28A745"}
     */
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    /**
     * Creates the show and hide animations and the auto-hide timer.
     * Top positions slide in from above, bottom positions from below, others only fade.
     */
    private void setupAnimations() {
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
            notifyHidden();
        });

        if (config.isAutoHide()) {
            autoHideTimeline = new Timeline(new KeyFrame(
                config.getDuration(),
                e -> hide()
            ));
            autoHideTimeline.setCycleCount(1);
        }
    }

    /**
     * Runs the hidden callback once and stops mirroring the owner theme.
     */
    private void notifyHidden() {
        unbindOwnerTheme();
        if (hiddenNotified) {
            return;
        }
        hiddenNotified = true;
        Runnable callback = config.getOnHidden();
        if (callback != null) {
            callback.run();
        }
    }

    /**
     * Registers this toast in the stack of its position and group and repositions the stack.
     * The key is remembered so the toast can always be removed from the stack it joined.
     */
    private void addToActiveToasts() {
        registeredKey = stackKey();
        activeToasts.computeIfAbsent(registeredKey, k -> new CopyOnWriteArrayList<>()).add(this);
        adjustToastPositions(registeredKey);
    }

    /**
     * Removes this toast from the stack it was registered in and
     * moves the remaining toasts of that stack to close the gap.
     */
    private void removeFromActiveToasts() {
        String key = registeredKey;
        if (key == null) {
            return;
        }
        registeredKey = null;
        List<Toast> toasts = activeToasts.get(key);
        if (toasts != null) {
            toasts.remove(this);
            if (toasts.isEmpty()) {
                activeToasts.remove(key);
            } else {
                adjustToastPositions(key);
            }
        }
    }

    /**
     * Measures this toast again and repositions its stack. Runs when the content height changes.
     */
    private void refresh() {
        if (!popup.isShowing()) {
            return;
        }
        measure();
        adjustToastPositions(registeredKey);
    }
    
    /**
     * Called when the popup window changes size. The real window size replaces the estimate,
     * and the stack is positioned again, so toasts never stay misaligned after the first layout pass.
     */
    private void onPopupResized() {
        if (!popup.isShowing() || popup.getWidth() <= 0 || popup.getHeight() <= 0) {
            return;
        }
        if (popup.getWidth() == actualWidth && popup.getHeight() == actualHeight) {
            return;
        }
        actualWidth = popup.getWidth();
        actualHeight = popup.getHeight();
        adjustToastPositions(registeredKey);
    }

    /**
     * Recomputes the screen coordinates of every visible toast in a stack.
     * Toasts are stacked in creation order using their measured heights plus {@link #TOAST_SPACING}.
     * Toasts that are not showing yet are skipped. Runs on the JavaFX application thread.
     *
     * @param key the stack key, ignored when {@code null}
     */
    private void adjustToastPositions(String key) {
        if (key == null) {
            return;
        }
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> adjustToastPositions(key));
            return;
        }

        List<Toast> toasts = activeToasts.get(key);
        if (toasts == null) return;

        Screen screen = getCurrentScreen();
        if (screen == null) return;

        Rectangle2D bounds = screen.getVisualBounds();
        double stacked = 0;

        for (Toast toast : new ArrayList<>(toasts)) {
            if (!toast.popup.isShowing()) continue;
            toast.popup.setX(toast.calculateX(bounds));
            toast.popup.setY(toast.calculateY(bounds, stacked));
            stacked += toast.visibleHeight() + TOAST_SPACING;
        }
    }

    /**
     * Finds the screen that contains the owner window.
     *
     * @return that screen, or the primary screen if the owner is missing or not on any screen
     */
    private Screen getCurrentScreen() {
        if (owner != null) {
            for (Screen screen : Screen.getScreens()) {
                if (screen.getBounds().contains(owner.getX(), owner.getY())) {
                    return screen;
                }
            }
        }
        return Screen.getPrimary();
    }

    /**
     * Measures the toast after CSS and layout are applied. The popup window size is preferred because it
     * includes the shadow margin and is what is actually positioned on screen; the content pane size is
     * used only until the window has a size.
     */
    private void measure() {
        rootPane.applyCss();
        rootPane.layout();
        double windowWidth = popup.getWidth();
        double windowHeight = popup.getHeight();
        actualWidth = windowWidth > 0 ? windowWidth : rootPane.prefWidth(-1);
        actualHeight = windowHeight > 0 ? windowHeight : rootPane.prefHeight(actualWidth);
    }

    private double visibleHeight() {
        Insets insets = rootPane.getInsets();
        return Math.max(0, actualHeight - insets.getTop() - insets.getBottom());
    }
    
    private double calculateX(Rectangle2D bounds) {
        ToastPosition position = config.getPosition();
        Insets insets = rootPane.getInsets();
        if (position.isCenter()) {
            return bounds.getMinX() + (bounds.getWidth() - actualWidth) / 2 + config.getOffsetX();
        }
        if (position.isRight()) {
            return bounds.getMaxX() - actualWidth - config.getOffsetX() + insets.getRight();
        }
        return bounds.getMinX() + config.getOffsetX() - insets.getLeft();
    }

    private double calculateY(Rectangle2D bounds, double stacked) {
        ToastPosition position = config.getPosition();
        Insets insets = rootPane.getInsets();
        if (position.isMiddle()) {
            return bounds.getMinY() + (bounds.getHeight() - actualHeight) / 2 + config.getOffsetY() + stacked;
        }
        if (position.isBottom()) {
            return bounds.getMaxY() - actualHeight - config.getOffsetY() + insets.getBottom() - stacked;
        }
        return bounds.getMinY() + config.getOffsetY() - insets.getTop() + stacked;
    }

    /**
     * Creates and shows an invisible toast once, so the first real toast does not pay for native window
     * creation, stylesheet processing and effect initialisation. It can be called before the owner is
     * showing; the warm-up then runs as soon as the owner is shown.
     *
     * @param owner the window toasts will be shown on
     */
    public static void warmUp(Window owner) {
        if (owner == null) {
            return;
        }
        if (!owner.isShowing()) {
            owner.showingProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean previous, Boolean current) {
                    if (current) {
                        owner.showingProperty().removeListener(this);
                        warmUp(owner);
                    }
                }
            });
            return;
        }
        Platform.runLater(() -> {
            Toast toast = builder()
                .owner(owner)
                .type(ToastType.CUSTOM)
                .title(" ")
                .message(" ")
                .showProgress(true)
                .autoHide(false)
                .group("warm-up")
                .build();
            toast.rootPane.setOpacity(0);
            toast.popup.show(owner, 0, 0);
            toast.measure();
            PauseTransition pause = new PauseTransition(Duration.millis(150));
            pause.setOnFinished(e -> toast.hideImmediately());
            pause.play();
        });
    }
    
    public void show() {
        Platform.runLater(() -> {
            if (popup.isShowing()) return;

            rootPane.setOpacity(0);
            popup.show(owner, 0, 0);
            measure();
            adjustToastPositions(registeredKey);

            Platform.runLater(() -> {
                refresh();
                showAnimation.play();
                if (config.isAutoHide() && autoHideTimeline != null) {
                    autoHideTimeline.play();
                }
            });
        });
    }

    public void showImmediately() {
        Platform.runLater(() -> {
            if (popup.isShowing()) return;

            rootPane.setOpacity(0);
            popup.show(owner, 0, 0);
            measure();
            adjustToastPositions(registeredKey);

            Platform.runLater(() -> {
                refresh();
                rootPane.setOpacity(1);
                if (config.isAutoHide() && autoHideTimeline != null) {
                    autoHideTimeline.play();
                }
            });
        });
    }

    /**
     * Hides the toast with a fade and slide animation.
     * The remaining toasts of the same stack move to close the gap.
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
     * Hides the toast instantly without animation.
     */
    public void hideImmediately() {
        Platform.runLater(() -> {
            if (autoHideTimeline != null) {
                autoHideTimeline.stop();
            }
            removeFromActiveToasts();
            popup.hide();
            notifyHidden();
        });
    }

    /**
     * Replaces the message text. Has no visible effect on a toast that uses custom content.
     *
     * @param message the new message
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    /**
     * Replaces the title text.
     *
     * @param title the new title
     */
    public void updateTitle(String title) {
        Platform.runLater(() -> titleLabel.setText(title));
    }

    /**
     * Sets the progress of the progress bar. Has no effect unless
     * {@link ToastConfig#showProgress(boolean)} was enabled.
     *
     * @param progress a value from 0 to 1, or a negative value for indeterminate progress
     */
    public void updateProgress(double progress) {
        Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress < 0 ? ProgressBar.INDETERMINATE_PROGRESS : Math.min(1, progress));
            }
        });
    }

    /**
     * Moves the toast to another position.
     * The toast leaves the stack it was in and joins the stack of the new position.
     *
     * @param newPosition the new position
     */
    public void changePosition(ToastPosition newPosition) {
        removeFromActiveToasts();
        config.position(newPosition);
        addToActiveToasts();
    }

    /**
     * Returns the configuration of this toast.
     *
     * @return the configuration
     */
    public ToastConfig getConfig() {
        return config;
    }

    /**
     * Creates a new builder for configuring and showing a toast.
     *
     * @return a new builder
     */
    public static ToastBuilder builder() {
        return new ToastBuilder();
    }

    /**
     * Immediately hides all active toasts in every stack.
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
     * Fluent builder for {@link Toast}. Each method mirrors the matching setter of {@link ToastConfig}.
     * If no owner is set, the focused window is used, or the last showing window.
     */
    public static class ToastBuilder {
        private Window owner;
        private ToastConfig config = new ToastConfig();

        /**
         * Sets the owner window.
         *
         * @param owner the owner window
         * @return this builder
         */
        public ToastBuilder owner(Window owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the owner stage.
         *
         * @param owner the owner stage
         * @return this builder
         */
        public ToastBuilder owner(Stage owner) {
            this.owner = owner;
            return this;
        }

        /**
         * @param title the title, or {@code null} for the default title of the type
         * @return this builder
         */
        public ToastBuilder title(String title) {
            config.title(title);
            return this;
        }

        /**
         * @param message the message text
         * @return this builder
         */
        public ToastBuilder message(String message) {
            config.message(message);
            return this;
        }

        /**
         * @param singleLineMessage {@code true} to keep the message on one line and cut it with an ellipsis,
         *                          so the toast height stays constant while the text changes
         * @return this builder
         */
        public ToastBuilder singleLineMessage(boolean singleLineMessage) {
            config.singleLineMessage(singleLineMessage);
            return this;
        }
        
        /**
         * @param position the screen position
         * @return this builder
         */
        public ToastBuilder position(ToastPosition position) {
            config.position(position);
            return this;
        }

        /**
         * @param duration the visible time when auto-hide is enabled
         * @return this builder
         */
        public ToastBuilder duration(Duration duration) {
            config.duration(duration);
            return this;
        }

        /**
         * @param offsetX the horizontal offset in pixels
         * @return this builder
         */
        public ToastBuilder offsetX(double offsetX) {
            config.offsetX(offsetX);
            return this;
        }

        /**
         * @param offsetY the vertical offset in pixels
         * @return this builder
         */
        public ToastBuilder offsetY(double offsetY) {
            config.offsetY(offsetY);
            return this;
        }

        /**
         * @param type the toast type
         * @return this builder
         */
        public ToastBuilder type(ToastType type) {
            config.type(type);
            return this;
        }

        /**
         * @param showIcon {@code true} to show the type icon
         * @return this builder
         */
        public ToastBuilder showIcon(boolean showIcon) {
            config.showIcon(showIcon);
            return this;
        }

        /**
         * @param showCloseButton {@code true} to show the close button in the top-right corner
         * @return this builder
         */
        public ToastBuilder showCloseButton(boolean showCloseButton) {
            config.showCloseButton(showCloseButton);
            return this;
        }

        /**
         * @param autoHide {@code true} to hide the toast after the configured duration
         * @return this builder
         */
        public ToastBuilder autoHide(boolean autoHide) {
            config.autoHide(autoHide);
            return this;
        }

        /**
         * @param showProgress {@code true} to show a progress bar driven by {@link Toast#updateProgress(double)}
         * @return this builder
         */
        public ToastBuilder showProgress(boolean showProgress) {
            config.showProgress(showProgress);
            return this;
        }

        /**
         * @param group the stack group name, or {@code null} for the default stack
         * @return this builder
         */
        public ToastBuilder group(String group) {
            config.group(group);
            return this;
        }

        /**
         * @param content a custom node shown instead of the message
         * @return this builder
         */
        public ToastBuilder content(Node content) {
            config.content(content);
            return this;
        }

        /**
         * @param onDismiss the callback run when the user clicks the close button
         * @return this builder
         */
        public ToastBuilder onDismiss(Runnable onDismiss) {
            config.onDismiss(onDismiss);
            return this;
        }

        /**
         * @param onHidden the callback run once after the toast is hidden
         * @return this builder
         */
        public ToastBuilder onHidden(Runnable onHidden) {
            config.onHidden(onHidden);
            return this;
        }

        /**
         * Sets the fixed toast width, capped by the maximum width.
         *
         * @param width the width in pixels
         * @return this builder
         */
        public ToastBuilder width(double width) {
            config.width(width);
            return this;
        }

        /**
         * @param maxWidth the maximum width in pixels
         * @return this builder
         */
        public ToastBuilder maxWidth(double maxWidth) {
            config.maxWidth(maxWidth);
            return this;
        }

        /**
         * @param maxHeight the maximum height in pixels
         * @return this builder
         */
        public ToastBuilder maxHeight(double maxHeight) {
            config.maxHeight(maxHeight);
            return this;
        }

        /**
         * @param customStyleClass an extra CSS style class for the toast container
         * @return this builder
         */
        public ToastBuilder customStyleClass(String customStyleClass) {
            config.customStyleClass(customStyleClass);
            return this;
        }

        /**
         * @param backgroundColor the background color overriding the stylesheet for this toast only
         * @return this builder
         */
        public ToastBuilder backgroundColor(Color backgroundColor) {
            config.backgroundColor(backgroundColor);
            return this;
        }

        /**
         * @param textColor the text color overriding the stylesheet for this toast only
         * @return this builder
         */
        public ToastBuilder textColor(Color textColor) {
            config.textColor(textColor);
            return this;
        }
        

        /**
         * Creates the toast without showing it. Without an explicit owner, the focused window
         * is used, or else the last showing window; other toasts and popups are never used.
         *
         * @return the new toast
         */
        public Toast build() {
            if (owner == null) {
                List<Window> candidates = Window.getWindows().stream()
                    .filter(w -> w.isShowing() && !(w instanceof PopupWindow))
                    .collect(Collectors.toList());
                owner = candidates.stream()
                    .filter(Window::isFocused)
                    .findFirst()
                    .orElse(candidates.isEmpty() ? null : candidates.get(candidates.size() - 1));
            }
            return new Toast(owner, config);
        }

        /**
         * Creates the toast and shows it with animation.
         */
        public void show() {
            build().show();
        }
    }
}