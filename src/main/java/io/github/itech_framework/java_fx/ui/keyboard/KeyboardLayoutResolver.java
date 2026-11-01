package io.github.itech_framework.java_fx.ui.keyboard;

import javafx.scene.control.TextInputControl;

@FunctionalInterface
public interface KeyboardLayoutResolver {
    String resolve(TextInputControl input);
}