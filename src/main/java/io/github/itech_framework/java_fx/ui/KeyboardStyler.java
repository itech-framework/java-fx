package io.github.itech_framework.java_fx.ui;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.util.Duration;

public class KeyboardStyler {
    public static void applyModernStyle() {
        PauseTransition delay = new PauseTransition(Duration.millis(200));
        delay.setOnFinished(event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof PopupWindow) {
                    Scene scene = window.getScene();
                    if (scene != null && scene.getRoot().lookup(".fxvk") != null) {
                        scene.getStylesheets().add(
                            KeyboardStyler.class.getResource("/static/css/keyboard.css").toExternalForm()
                        );
                        System.out.println("Modern keyboard style applied");
                        break;
                    }
                }
            }
        });
        delay.play();
    }
}