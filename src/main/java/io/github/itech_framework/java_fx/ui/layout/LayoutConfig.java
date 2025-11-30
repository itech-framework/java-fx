package io.github.itech_framework.java_fx.ui.layout;

public class LayoutConfig {
	private String layoutFxml;
    private String contentContainerId = "contentContainer";
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
