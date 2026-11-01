package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxResources;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Window;

public final class KeyboardController {
	private static final double MARGIN = 8;
	private static final String DEFAULT_STYLESHEET = "/static/css/keyboard.css";

	private static KeyboardController instance;

	private final Popup popup = new Popup();
	private final VirtualKeyboard keyboard;
	private final Set<Window> windows = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<Scene> scenes = Collections.newSetFromMap(new WeakHashMap<>());
	private final ChangeListener<Node> focusListener = (observable, previous, current) -> onFocus(current);
	private final ChangeListener<Object> changed = (observable, previous, current) -> schedule();
	private final EventHandler<MouseEvent> clickFilter = this::onClick;

	private Window trackedWindow;
	private TextInputControl trackedInput;
	private Node trackedAnchor;
	private boolean pending;

	private KeyboardController() {
		FxSettings.Keyboard settings = FxSettings.get().keyboard();
		keyboard = new VirtualKeyboard(settings.styleClass());
		keyboard.getStyleClass().add("root");
		if (settings.inheritTheme()) {
			FxResources.applyTheme(keyboard.getStylesheets());
		}
		addStylesheet(defaultStylesheet());
		if (!settings.stylesheet().isBlank()) {
			addStylesheet(userStylesheet(settings.stylesheet()));
		}
		keyboard.setOnHide(this::hide);
		keyboard.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> schedule());
		popup.setAutoHide(false);
		popup.setAutoFix(false);
		popup.setHideOnEscape(false);
		popup.getContent().add(keyboard);
	}

	public static synchronized void install() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(KeyboardController::install);
			return;
		}
		if (instance != null) {
			return;
		}
		instance = new KeyboardController();
		instance.start();
	}

	private void start() {
		for (Window window : new ArrayList<>(Window.getWindows())) {
			attach(window);
		}
		Window.getWindows().addListener((ListChangeListener<Window>) change -> {
			while (change.next()) {
				for (Window window : change.getAddedSubList()) {
					attach(window);
				}
				for (Window window : change.getRemoved()) {
					detach(window);
				}
			}
		});
	}

	private void attach(Window window) {
		if (window instanceof PopupWindow || !windows.add(window)) {
			return;
		}
		window.sceneProperty().addListener((observable, previous, current) -> {
			if (previous != null) {
				unbind(previous);
			}
			if (current != null) {
				bind(current);
			}
		});
		if (window.getScene() != null) {
			bind(window.getScene());
		}
	}

	private void detach(Window window) {
		windows.remove(window);
		if (popup.getOwnerWindow() == window) {
			hide();
		}
		if (window.getScene() != null) {
			unbind(window.getScene());
		}
	}

	private void bind(Scene scene) {
		if (!scenes.add(scene)) {
			return;
		}
		scene.focusOwnerProperty().addListener(focusListener);
		scene.addEventFilter(MouseEvent.MOUSE_PRESSED, clickFilter);
	}

	private void unbind(Scene scene) {
		if (scenes.remove(scene)) {
			scene.focusOwnerProperty().removeListener(focusListener);
			scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, clickFilter);
		}
	}

	private void onFocus(Node current) {
		if (current instanceof TextInputControl input) {
			show(input);
		} else {
			hide();
		}
	}

	private void onClick(MouseEvent event) {
		Node node = event.getTarget() instanceof Node target ? target : null;
		while (node != null && !(node instanceof TextInputControl)) {
			node = node.getParent();
		}
		if (node instanceof TextInputControl input) {
			Platform.runLater(() -> {
				if (input.isFocused()) {
					show(input);
				}
			});
		}
	}

	private boolean eligible(TextInputControl input) {
		if (!input.isEditable() || input.isDisabled() || !input.isVisible()) {
			return false;
		}
		Scene scene = input.getScene();
		if (scene == null || scene.getWindow() == null || !scene.getWindow().isShowing()) {
			return false;
		}
		if (input.getProperties().get(KeyboardOptions.PROPERTY) instanceof KeyboardOptions options) {
			return options.enabled();
		}
		return KeyboardConfig.current().scope() == KeyboardScope.ALL;
	}

	private void show(TextInputControl input) {
		if (!eligible(input)) {
			hide();
			return;
		}
		Window window = input.getScene().getWindow();
		KeyboardOptions options = KeyboardOptions.of(input);
		if (popup.isShowing() && keyboard.getTarget() == input) {
			syncDark(input);
			place();
			return;
		}
		if (popup.isShowing() && popup.getOwnerWindow() != window) {
			popup.hide();
		}
		KeyboardConfig config = KeyboardConfig.current();
		keyboard.setKeyHeight(options.keyHeight());
		keyboard.setGap(options.keyGap());
		keyboard.setMaxKeyWidth(options.maxKeyWidth());
		keyboard.setCycle(options.layouts());
		String layoutId = options.layout() == null || options.layout().isBlank() ? config.layout() : options.layout();
		keyboard.setLayout(KeyboardLayouts.resolve(input, layoutId));
		keyboard.setTarget(input);
		syncDark(input);
		track(window, input, KeyboardAnchors.find(input, options.anchor()));
		place();
		schedule();
	}

	private void hide() {
		untrack();
		if (popup.isShowing()) {
			popup.hide();
		}
		keyboard.setTarget(null);
	}

	private void schedule() {
		if (pending) {
			return;
		}
		pending = true;
		Platform.runLater(() -> {
			pending = false;
			if (popup.isShowing()) {
				place();
			}
		});
	}

	private void place() {
		TextInputControl input = keyboard.getTarget();
		if (input == null || !eligible(input)) {
			hide();
			return;
		}
		Window window = input.getScene().getWindow();
		KeyboardOptions options = KeyboardOptions.of(input);
		Bounds anchored = KeyboardAnchors.anchorBounds(input, options.anchor());
		Bounds area = anchored != null ? anchored
				: new BoundingBox(window.getX(), window.getY(), window.getWidth(), window.getHeight());
		Rectangle2D screen = screenOf(area);

		double width = area.getWidth();
		if (options.maxWidth() > 0) {
			width = Math.min(width, options.maxWidth());
		}
		if (options.minWidth() > 0) {
			width = Math.max(width, options.minWidth());
		}
		width = Math.min(width, keyboard.naturalWidth());
		width = Math.min(width, screen.getWidth());

		keyboard.setPrefWidth(width);
		keyboard.applyCss();
		double height = keyboard.prefHeight(width);

		double x = area.getMinX() + (area.getWidth() - width) / 2;
		double y;
		if (anchored != null) {
			double below = area.getMaxY() + MARGIN;
			y = below + height <= screen.getMaxY() ? below : area.getMaxY() - height;
		} else {
			y = area.getMaxY() - height;
		}

		Bounds field = input.localToScreen(input.getLayoutBounds());
		if (field != null && new BoundingBox(x, y, width, height).intersects(field)) {
			y = field.getMinY() - MARGIN - height;
		}

		x = clamp(x, screen.getMinX(), screen.getMaxX() - width);
		y = clamp(y, screen.getMinY(), screen.getMaxY() - height);

		if (popup.isShowing()) {
			popup.setX(x);
			popup.setY(y);
		} else {
			popup.show(window, x, y);
		}
	}

	private void track(Window window, TextInputControl input, Node anchor) {
		untrack();
		trackedWindow = window;
		trackedInput = input;
		trackedAnchor = anchor;
		window.xProperty().addListener(changed);
		window.yProperty().addListener(changed);
		window.widthProperty().addListener(changed);
		window.heightProperty().addListener(changed);
		window.showingProperty().addListener(changed);
		input.editableProperty().addListener(changed);
		input.disableProperty().addListener(changed);
		input.visibleProperty().addListener(changed);
		if (anchor != null) {
			anchor.layoutBoundsProperty().addListener(changed);
		}
	}

	private void untrack() {
		if (trackedWindow != null) {
			trackedWindow.xProperty().removeListener(changed);
			trackedWindow.yProperty().removeListener(changed);
			trackedWindow.widthProperty().removeListener(changed);
			trackedWindow.heightProperty().removeListener(changed);
			trackedWindow.showingProperty().removeListener(changed);
			trackedWindow = null;
		}
		if (trackedInput != null) {
			trackedInput.editableProperty().removeListener(changed);
			trackedInput.disableProperty().removeListener(changed);
			trackedInput.visibleProperty().removeListener(changed);
			trackedInput = null;
		}
		if (trackedAnchor != null) {
			trackedAnchor.layoutBoundsProperty().removeListener(changed);
			trackedAnchor = null;
		}
	}

	private void syncDark(TextInputControl input) {
		String dark = darkClass();
		if (dark == null || dark.isBlank()) {
			return;
		}
		Parent root = input.getScene().getRoot();
		if (root.getStyleClass().contains(dark)) {
			if (!keyboard.getStyleClass().contains(dark)) {
				keyboard.getStyleClass().add(dark);
			}
		} else {
			keyboard.getStyleClass().remove(dark);
		}
	}

	private static Rectangle2D screenOf(Bounds area) {
		List<Screen> screens = Screen.getScreensForRectangle(area.getMinX(), area.getMinY(),
				Math.max(1, area.getWidth()), Math.max(1, area.getHeight()));
		Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
		return screen.getVisualBounds();
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(value, max));
	}

	private static String darkClass() {
		return FxSettings.get().theme().darkStyleClass();
	}

	static String defaultStylesheet() {
		return FxResources.require(KeyboardController.class, DEFAULT_STYLESHEET, "built-in keyboard stylesheet")
				.toExternalForm();
	}

	static String userStylesheet(String path) {
		return FxResources.require(KeyboardController.class, path, "keyboard stylesheet (fx.keyboard.stylesheet)")
				.toExternalForm();
	}

	private void addStylesheet(String url) {
		if (!keyboard.getStylesheets().contains(url)) {
			keyboard.getStylesheets().add(url);
		}
	}
}