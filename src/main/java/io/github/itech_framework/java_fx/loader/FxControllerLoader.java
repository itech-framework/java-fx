package io.github.itech_framework.java_fx.loader;

import javafx.fxml.FXMLLoader;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;

public class FxControllerLoader {
    public static <T> T load(Class<?> contextClass, String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(contextClass.getResource(fxmlPath));
        T t = loader.load();
        if(ComponentStore.getComponent(loader.getController().getClass()).isEmpty()){
            throw new IllegalStateException("Controller not registered!");
        }
        
        ComponentProcessor.injectFields(loader.getController().getClass(), loader.getController());
        ComponentProcessor.injectMethods(loader.getController().getClass(), loader.getController(), ComponentProcessor.PRESENTATION_LEVEL);

        return t;
    }

}
