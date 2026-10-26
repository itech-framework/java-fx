package io.github.itech_framework.java_fx;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.annotations.ComponentScan;
import io.github.itech_framework.core.annotations.jfx.EnableJavaFx;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.resourcecs.CleanupRegistry;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.core.utils.PropertiesLoader;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.processor.JavaFxComponentProcessor;
import io.github.itech_framework.java_fx.router.Router;
import io.github.itech_framework.java_fx.ui.FrameworkPreloader;
import io.github.itech_framework.java_fx.ui.SmoothScroll;
import io.github.itech_framework.java_fx.utils.concurrent.BackgroundTaskService;
import io.github.itech_framework.java_fx.utils.notification.NotificationCenter;
import io.github.itech_framework.java_fx.utils.notification.SystemNotificationService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader.ErrorNotification;
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
		Objects.requireNonNull(clazz, "application class must not be null");
		if (!clazz.isAnnotationPresent(EnableJavaFx.class)) {
			throw new FrameworkException("To run JavaFx application please use the @EnableJavaFx on main class!");
		}
		mainClass = clazz;

		ComponentScan scan = clazz.getAnnotation(ComponentScan.class);
		if (scan != null) {
			PropertiesLoader.load(scan.properties(), clazz);
		}
		FxSettings settings = FxSettings.reload();

		if (settings.preloader().enabled() && System.getProperty("javafx.preloader") == null) {
			System.setProperty("javafx.preloader", FrameworkPreloader.class.getName());
		}

		if (settings.app().smoothFont()) {
			System.setProperty("prism.lcdtext", "false");
		}

		Application.launch(clazz, args);
	}

	@Override
	public final void init() throws Exception {
	    try {
	        progress(0.1);
	        if (!ComponentStore.hasComponent(Router.class)) {
	            ComponentStore.registerComponent(Router.class.getName(), router, ComponentProcessor.DEFAULT_LEVEL);
	        }
	        progress(0.2);
	        JavaFxComponentProcessor.initializeFxControllers(mainClass);
	        progress(0.5);
	        ComponentProcessor.injectFields(mainClass, this);
	        progress(0.7);
	        ComponentProcessor.injectMethods(mainClass, this, ComponentProcessor.PRESENTATION_LEVEL);
	        progress(0.9);
	        onInit();
	        configureScrolling();
	        progress(1.0);
	    } catch (Exception e) {
	        onStartupFailure(e);
	        throw e;
	    }
	}

	private void configureScrolling() {
		FxSettings.Scroll scroll = FxSettings.get().scroll();
		Platform.runLater(() -> {
			try {
				SmoothScroll.apply(scroll);
			} catch (RuntimeException e) {
				logger.warn("Smooth scrolling could not be initialized", e);
			}
		});
	}

	protected abstract void onInit() throws Exception;

	protected void onStartupFailure(Exception error) {
		logger.error("Application startup failed", error);
		String details = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
		notifyPreloader(new ErrorNotification(null, details, error));
	}

	protected void onShutdown() throws Exception {
	}

	@Override
	public abstract void start(Stage primaryStage) throws Exception;

	@Override
	public void stop() throws Exception {
		logger.debug("Framework resources is cleaning up...");
		try {
			SmoothScroll.setEnabled(false);
			router.shutdown();
			onShutdown();
		} catch (Exception e) {
			logger.error("Error while shutting down the application", e);
		}
		if (FxSettings.get().app().exitOnStop()) {
			BackgroundTaskService.getInstance().shutdown();
			NotificationCenter.getInstance().stop();
			SystemNotificationService.getInstance().shutdown();
			Platform.exit();
			System.exit(0);
		} else {
			CleanupRegistry.cleanup();
		}
	}
	
	private void progress(double value) throws InterruptedException {
	    notifyPreloader(new ProgressNotification(value));
	    long delay = FxSettings.get().preloader().delay();
	    if (delay > 0) {
	        Thread.sleep(delay);
	    }
	}
	
}