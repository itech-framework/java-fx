package io.github.itech_framework.java_fx.ui.layout;

import javafx.scene.Parent;

public abstract class LayoutController {
	abstract public void  setContent(Parent content);
	abstract public Parent getContentContainer();
	abstract public void onLayoutLoaded();
}
