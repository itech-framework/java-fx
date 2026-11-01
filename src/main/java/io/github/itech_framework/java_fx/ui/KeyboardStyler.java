package io.github.itech_framework.java_fx.ui;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.util.Duration;

public class KeyboardStyler {
    private static final Logger logger = LogManager.getLogger(KeyboardStyler.class);
    private static final String STYLESHEET = "/static/css/keyboard.css";

    public static void applyModernStyle() {
        URL stylesheet = KeyboardStyler.class.getResource(STYLESHEET);
        if (stylesheet == null) {
            logger.warn("Keyboard stylesheet not found: {}", STYLESHEET);
            return;
        }
        String css = stylesheet.toExternalForm();
        PauseTransition delay = new PauseTransition(Duration.millis(200));
        delay.setOnFinished(event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof PopupWindow) {
                    Scene scene = window.getScene();
                    if (scene != null && scene.getRoot().lookup(".fxvk") != null) {
                        if (!scene.getStylesheets().contains(css)) {
                            scene.getStylesheets().add(css);
                            logger.debug("Modern keyboard style applied");
                        }
                        break;
                    }
                }
            }
        });
        delay.play();
    }
}