package io.github.itech_framework.java_fx.loader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.store.ComponentStore;
import io.github.itech_framework.java_fx.utils.FxResources;
import javafx.fxml.FXMLLoader;

public final class FxControllerLoader {

    private FxControllerLoader() {
    }

    public static <T> T load(Class<?> contextClass, String fxmlPath) throws Exception {
        Result<T, Object> result = loadWithResult(contextClass, fxmlPath);
        return result.getRoot();
    }

    @SuppressWarnings("unchecked")
    public static <T, C> Result<T, C> loadWithResult(Class<?> contextClass, String fxmlPath) throws Exception {
        URL location = FxResources.require(contextClass, fxmlPath, "FXML view");

        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(FxControllerLoader::createController);
        T root = loader.load();

        Object controller = loader.getController();
        if (controller == null) {
            throw new FrameworkException("No controller found for " + fxmlPath
                    + ". Add fx:controller=\"your.package.YourController\" to the root element of the FXML file.");
        }
        Class<?> controllerType = controller.getClass();
        if (ComponentStore.getComponent(controllerType).isEmpty()) {
            throw new FrameworkException("Controller " + controllerType.getName() + " used by " + fxmlPath
                    + " is not registered. Annotate it with @FxController and keep it inside the @ComponentScan package.");
        }

        ComponentProcessor.runInitMethods(controllerType, controller);
        return new Result<>(root, (C) controller);
    }

    private static Object createController(Class<?> type) {
        Object instance;
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new FrameworkException("FXML controller " + type.getName()
                    + " needs a no-argument constructor. FXMLLoader creates controllers itself; "
                    + "inject services with @Use fields instead of constructor parameters.", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            throw new FrameworkException("Failed to create FXML controller " + type.getName() + ": "
                    + cause.getMessage(), cause instanceof Exception ex ? ex : e);
        } catch (ReflectiveOperationException e) {
            throw new FrameworkException("Failed to create FXML controller " + type.getName(), e);
        }

        if (ComponentStore.hasComponent(type)) {
            ComponentProcessor.injectFields(type, instance);
        }
        return instance;
    }

    public static void dispose(Object controller) {
        if (controller != null) {
            ComponentProcessor.destroyInstance(controller.getClass(), controller);
        }
    }

    public static class Result<T, C> {
        private T root;
        private C controller;

        public Result(T root, C controller) {
            this.root = root;
            this.controller = controller;
        }

        public T getRoot() {
            return root;
        }

        public void setRoot(T root) {
            this.root = root;
        }

        public C getController() {
            return controller;
        }

        public void setController(C controller) {
            this.controller = controller;
        }
    }
}