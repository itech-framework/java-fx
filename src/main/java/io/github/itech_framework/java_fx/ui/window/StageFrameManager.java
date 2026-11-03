package io.github.itech_framework.java_fx.ui.window;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxTheme;
import io.github.itech_framework.java_fx.utils.FxThread;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class StageFrameManager {
    private static final Logger logger = LogManager.getLogger(StageFrameManager.class);
    private static final String ATTACHED_KEY = "itech.window.frame.attached";
    private static volatile boolean installed;

    private static volatile Supplier<Boolean> darkMode = FxTheme::storedDarkMode;

    private static ListChangeListener<Window> windowListener;

    private StageFrameManager() {
    }

    public static synchronized void install() {
        if (installed || !FxSettings.get().windowFrame().darkFrame()) {
            return;
        }
        if (!WindowFrameThemes.get().isSupported()) {
            logger.debug("Dark window frame is not supported on this system");
            return;
        }
        installed = true;
        for (Window window : new ArrayList<>(Window.getWindows())) {
            attach(window);
        }
        windowListener = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window window : change.getAddedSubList()) {
                        attach(window);
                    }
                }
            }
        };
        Window.getWindows().addListener(windowListener);
    }

    public static void refresh() {
        if (!installed) {
            return;
        }
        FxThread.run(() -> {
            for (Window window : new ArrayList<>(Window.getWindows())) {
                if (window instanceof Stage stage && stage.isShowing()) {
                    apply(stage);
                }
            }
        });
    }

    private static void attach(Window window) {
        if (!(window instanceof Stage stage)) {
            return;
        }
        if (stage.getProperties().putIfAbsent(ATTACHED_KEY, Boolean.TRUE) != null) {
            return;
        }
        stage.showingProperty().addListener((observable, wasShowing, showing) -> {
            if (showing) {
                Platform.runLater(() -> apply(stage));
            }
        });
        if (stage.isShowing()) {
            Platform.runLater(() -> apply(stage));
        }
    }

    private static void apply(Stage stage) {
        if (!stage.isShowing()) {
            return;
        }
        Boolean dark = darkMode.get();
        if (dark == null) {
            return;
        }
        try {
            WindowFrameThemes.get().apply(stage, dark);
        } catch (RuntimeException | LinkageError e) {
            logger.warn("Window frame theme could not be applied", e);
        }
    }

    static void useDarkModeSource(Supplier<Boolean> source) {
        darkMode = source;
    }

    static synchronized void resetForTests() {
        if (windowListener != null) {
            Window.getWindows().removeListener(windowListener);
            windowListener = null;
        }
        installed = false;
        darkMode = FxTheme::storedDarkMode;
    }
}