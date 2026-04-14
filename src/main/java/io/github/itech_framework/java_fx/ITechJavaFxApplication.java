package io.github.itech_framework.java_fx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.itech_framework.core.annotations.jfx.EnableJavaFx;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.java_fx.processor.JavaFxComponentProcessor;
import io.github.itech_framework.java_fx.router.Router;
import io.github.itech_framework.java_fx.ui.FrameworkPreloader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.ProgressNotification;
import javafx.stage.Stage;

public abstract class ITechJavaFxApplication extends Application {
    private static final Logger logger = LogManager.getLogger(ITechJavaFxApplication.class);
    public static final Router router = new Router();
    private static Class<? extends ITechJavaFxApplication> mainClass;

    public static Class<? extends ITechJavaFxApplication> getMainClass() {
        return mainClass;
    }

    public static void run(Class<? extends ITechJavaFxApplication> clazz, String[] args) throws Exception {
        if (!clazz.isAnnotationPresent(EnableJavaFx.class)) {
            throw new FrameworkException("To run JavaFx application please use the @EnableJavaFx on main class!");
        }
        mainClass = clazz;
        System.setProperty("javafx.preloader", FrameworkPreloader.class.getName());
        Application.launch(clazz, args);
    }

    @Override
    public final void init() throws Exception {
        notifyPreloader(new ProgressNotification(0.2));
        JavaFxComponentProcessor.initializeFxControllers(mainClass);
        notifyPreloader(new ProgressNotification(0.5));
        ComponentProcessor.injectFields(mainClass, this);
        notifyPreloader(new ProgressNotification(0.7));
        ComponentProcessor.injectMethods(mainClass, this, ComponentProcessor.PRESENTATION_LEVEL);
        notifyPreloader(new ProgressNotification(0.9));
        ComponentStore.registerComponent(Router.class.getName(), router, ComponentProcessor.DEFAULT_LEVEL);
        onInit();
        notifyPreloader(new ProgressNotification(1.0));
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