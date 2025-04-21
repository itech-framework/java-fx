package io.github.itech_framework.java_fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.itech_framework.core.annotations.jfx.EnableJavaFx;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.processor.JavaFxComponentProcessor;
import io.github.itech_framework.java_fx.router.Router;

public abstract class ITechJavaFxApplication extends Application {
    private static final Logger logger = LogManager.getLogger(ITechJavaFxApplication.class);
    public static final Router router = new Router();

    public static void run(Class<? extends ITechJavaFxApplication> clazz, String[] args) throws Exception {

        if(!clazz.isAnnotationPresent(EnableJavaFx.class)){
            throw new FrameworkException("To run JavaFx application please use the @EnableJavaFx on main class!");
        }
        JavaFxComponentProcessor.initializeFxControllers(clazz);
        Application.launch(clazz, args);
    }

    @Override
    public final void init() throws Exception {
        ComponentProcessor.injectFields(getClass(), this);
        ComponentProcessor.injectMethods(getClass(), this, ComponentProcessor.PRESENTATION_LEVEL);
        onInit();
        // register router inside the components
        ComponentStore.registerComponent(Router.class.getName(), router, ComponentProcessor.DEFAULT_LEVEL);
    }

    protected abstract void onInit() throws Exception;

    @Override
    public abstract void start(Stage primaryStage) throws Exception;

    @Override
    public void stop() throws Exception {
        logger.debug("Framework resources is cleaning up...");
        Platform.exit();
        System.exit(0);
    }
}