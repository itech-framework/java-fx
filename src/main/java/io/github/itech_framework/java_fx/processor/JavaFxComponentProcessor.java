package io.github.itech_framework.java_fx.processor;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.java_fx.annotations.FxController;
import io.github.itech_framework.java_fx.config.FxSettings;

public class JavaFxComponentProcessor {

    public static void initializeFxControllers(Class<?> mainClass) throws Exception {
        ComponentProcessor.initialize(mainClass);
        if (FxSettings.get().app().controllersRequired()) {
            validateFxControllers();
        }
    }

    private static void validateFxControllers() {
        for (Object instance : ComponentStore.getAllComponents()) {
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