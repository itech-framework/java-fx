package io.github.itech_framework.java_fx.ui.window;

import javafx.stage.Stage;

final class NoOpWindowFrameTheme implements WindowFrameTheme {

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void apply(Stage stage, boolean dark) {
    }
}