package io.github.itech_framework.java_fx.ui.layout;

import io.github.itech_framework.java_fx.config.FxSettings;

public class LayoutConfig {
	private String layoutFxml;
	private String contentContainerId = FxSettings.get().router().contentContainerId();
    private boolean enabled = true;
    
    public LayoutConfig(String layoutFxml) {
        this.layoutFxml = layoutFxml;
    }
    
    public LayoutConfig(String layoutFxml, String contentContainerId) {
        this.layoutFxml = layoutFxml;
        this.contentContainerId = contentContainerId;
    }

	public String getLayoutFxml() {
		return layoutFxml;
	}

	public void setLayoutFxml(String layoutFxml) {
		this.layoutFxml = layoutFxml;
	}

	public String getContentContainerId() {
		return contentContainerId;
	}

	public void setContentContainerId(String contentContainerId) {
		this.contentContainerId = contentContainerId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
