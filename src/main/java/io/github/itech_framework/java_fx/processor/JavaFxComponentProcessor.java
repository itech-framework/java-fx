package io.github.itech_framework.java_fx.processor;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ApplicationContext;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.java_fx.annotations.FxController;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.router.Router;
import io.github.itech_framework.java_fx.ui.keyboard.KeyboardBootstrap;
import io.github.itech_framework.java_fx.utils.notification.NotificationCenter;
import io.github.itech_framework.java_fx.utils.notification.NotificationSource;

public class JavaFxComponentProcessor {

	public static void initializeFxControllers(Class<?> mainClass, Router router) throws Exception {
	    ApplicationContext context = ComponentProcessor.initialize(mainClass, store -> {
	        if (!store.hasComponent(Router.class)) {
	            store.registerComponent(Router.class.getName(), router, ComponentProcessor.DEFAULT_LEVEL);
	        }
	    });
	    try {
	        if (FxSettings.get().app().controllersRequired()) {
	            validateFxControllers(context);
	        }
	        registerNotificationSources(context);
	        KeyboardBootstrap.initialize(context);
	    } catch (RuntimeException e) {
	        context.close();
	        throw e;
	    }
	}

    private static void registerNotificationSources(ApplicationContext context) {
        NotificationCenter center = NotificationCenter.getInstance();
        for (Object instance : context.store().getAllComponents()) {
            if (instance instanceof NotificationSource source) {
                center.register(source);
            }
        }
    }

    private static void validateFxControllers(ApplicationContext context) {
        for (Object instance : context.store().getAllComponents()) {
            for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
                if (type.isAnnotationPresent(FxController.class)) {
                    return;
                }
            }
        }
        throw new FrameworkException("FX Controllers not found!\n"
                + "Ensure at least one class inside the @ComponentScan package is annotated with @FxController.\n"
                + "If this application has no FXML controllers, set fx.controllers.required=false.");
    }
}