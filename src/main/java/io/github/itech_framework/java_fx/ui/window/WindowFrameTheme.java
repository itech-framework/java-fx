package io.github.itech_framework.java_fx.ui.window;

import javafx.stage.Stage;

public interface WindowFrameTheme {

    boolean isSupported();

    void apply(Stage stage, boolean dark);
    
}