package org.itech.framework.fx.java_fx.processor;

import org.itech.framework.fx.core.processor.components_processor.ComponentProcessor;
import org.itech.framework.fx.core.store.ComponentStore;
import org.itech.framework.fx.exceptions.FrameworkException;
import org.itech.framework.fx.java_fx.annotations.FxController;
import org.itech.framework.fx.java_fx.router.Router;
import org.itech.framework.fx.utils.ReflectionUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFxComponentProcessor {
    public static void initializeFxControllers(Class<?> mainClass) throws Exception {
        ComponentProcessor.initialize(mainClass);
        // pre-handle
        validateFxControllers();
    }

    private static void validateFxControllers() {
        AtomicBoolean fxControllersFound = new AtomicBoolean(false);
        ComponentStore.components.forEach((key, instance) -> {
            if(instance.getClass().isAnnotationPresent(FxController.class)){
                fxControllersFound.set(true);
            }
        });
        if(!fxControllersFound.get()){
            throw new FrameworkException("Controllers are not found in the current javafx application!\n The fxml controller must be annotated with @FxController.");
        }
    }

}
