package io.github.itech_framework.java_fx.processor;

import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.annotations.FxController;

import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFxComponentProcessor {
    public static void initializeFxControllers(Class<?> mainClass) throws Exception {
        // Initialize core components
        ComponentProcessor.initialize(mainClass);

        // Validate FX controllers
        validateFxControllers();
    }


    private static void validateFxControllers() {
        AtomicBoolean fxControllersFound = new AtomicBoolean(false);

        ComponentStore.components.forEach((key, instance) -> {
            Class<?> clazz = instance.getClass();
            while (clazz != null) {
                if (clazz.isAnnotationPresent(FxController.class)) {
                    fxControllersFound.set(true);
                    break;
                }
                clazz = clazz.getSuperclass();
            }
        });

        if (!fxControllersFound.get()) {
            throw new FrameworkException(
                    "FX Controllers not found!\n" +
                            "Ensure at least one class in the component scan path is annotated with @FxController\n" +
                            "and extends javafx.fxml. Initializable or uses @FXML annotations."
            );
        }
    }
}