package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.List;

import javafx.scene.control.TextInputControl;

public record KeyboardOptions(boolean enabled, String layout, List<String> layouts, String anchor,
                              double keyHeight, double keyGap, double maxKeyWidth,
                              double minWidth, double maxWidth) {

    public static final String PROPERTY = "fx.keyboard.options";

    public static KeyboardOptions of(TextInputControl input) {
        if (input.getProperties().get(PROPERTY) instanceof KeyboardOptions options) {
            return options;
        }
        return defaults(input);
    }

    static KeyboardOptions defaults(TextInputControl input) {
        KeyboardConfig config = KeyboardConfig.current();
        return new KeyboardOptions(true, config.layoutFor(input), config.layouts(), config.anchorId(),
                config.keyHeight(), config.keyGap(), config.maxKeyWidth(), config.minWidth(), config.maxWidth());
    }
}