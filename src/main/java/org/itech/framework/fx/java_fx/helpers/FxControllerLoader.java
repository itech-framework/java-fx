package org.itech.framework.fx.java_fx.helpers;

import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadListener;
import org.itech.framework.fx.core.processor.components_processor.ComponentProcessor;
import org.itech.framework.fx.core.store.ComponentStore;

import java.util.concurrent.atomic.AtomicReference;

public class FxControllerLoader {
    public static <T> T load(Class<?> contextClass, String fxmlPath) throws Exception {
        FXMLLoader loader = new FXMLLoader(contextClass.getResource(fxmlPath));
        T t = loader.load();
        if(ComponentStore.getComponent(loader.getController().getClass()).isEmpty()){
            throw new IllegalStateException("Controller not registered!");
        }
        
        ComponentProcessor.injectFields(loader.getController().getClass(), loader.getController());
        ComponentProcessor.injectMethods(loader.getController().getClass(), loader.getController());

        return t;
    }
}
