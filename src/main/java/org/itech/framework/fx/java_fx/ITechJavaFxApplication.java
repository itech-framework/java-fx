package org.itech.framework.fx.java_fx;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.itech.framework.fx.core.annotations.jfx.EnableJavaFx;
import org.itech.framework.fx.core.processor.components_processor.ComponentProcessor;
import org.itech.framework.fx.core.store.ComponentStore;
import org.itech.framework.fx.exceptions.FrameworkException;
import org.itech.framework.fx.java_fx.processor.JavaFxComponentProcessor;
import org.itech.framework.fx.java_fx.router.Router;

public abstract class ITechJavaFxApplication extends Application {
    private static final Logger logger = LogManager.getLogger(ITechJavaFxApplication.class);
    public static final Router router = new Router();

    public static void run(Class<? extends ITechJavaFxApplication> clazz, String[] args) throws Exception {

        if(!clazz.isAnnotationPresent(EnableJavaFx.class)){
            throw new FrameworkException("To run JavaFx application please use the @EnableJavaFx on main class!");
        }

        JavaFxComponentProcessor.initializeFxControllers(clazz);

        // register router inside the components
        ComponentStore.registerComponent(Router.class.getName(), router, ComponentProcessor.DEFAULT_LEVEL);

        Application.launch(clazz, args);
    }

    @Override
    public abstract void init() throws Exception;

    @Override
    public abstract void start(Stage primaryStage) throws Exception;

    @Override
    public void stop() throws Exception {
        logger.debug("Framework resources cleaned up");
    }
}