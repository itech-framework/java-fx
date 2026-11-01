package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.List;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ApplicationContext;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxResources;

public final class KeyboardBootstrap {

    private KeyboardBootstrap() {}

    public static void initialize(ApplicationContext context) {
        FxSettings.Keyboard settings = FxSettings.get().keyboard();
        KeyboardConfig config = KeyboardConfig.from(settings);
        KeyboardConfig.install(config);
        List<Object> components = new ArrayList<>(context.store().getAllComponents());
        for (Object component : components) {
            if (component instanceof KeyboardConfigurer configurer) {
                configurer.configure(config);
            }
        }
        if (settings.enabled()) {
        	
        	List<String> problems = config.validate();
        	if (!problems.isEmpty()) {
        	    throw new FrameworkException("Invalid keyboard settings:\n - " + String.join("\n - ", problems));
        	}
        	
            if (settings.inheritTheme() && FxSettings.get().theme().enabled()) {
                FxResources.themeStylesheet();
            }
            KeyboardController.defaultStylesheet();
            if (!settings.stylesheet().isBlank()) {
                KeyboardController.userStylesheet(settings.stylesheet());
            }
        }
        KeyboardBinder.validate(components);
    }
}