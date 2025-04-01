package org.itech.framework.fx.java_fx.router;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.itech.framework.fx.core.store.ComponentStore;
import org.itech.framework.fx.java_fx.helpers.FxControllerLoader;
import org.itech.framework.fx.java_fx.router.config.Middleware;
import org.itech.framework.fx.java_fx.router.config.RouterConfig;
import org.itech.framework.fx.java_fx.router.config.TransitionEffect;
import org.itech.framework.fx.java_fx.router.core.Routable;
import org.itech.framework.fx.java_fx.router.core.Route;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Router {
    private final Map<String, Route> routes = new HashMap<>();
    private final Stack<Route> navigationStack = new Stack<>();
    private final RouterConfig config = new RouterConfig();
    private Stage primaryStage;
    private Class<?> primaryClass;

    public void initialize(Class<?> clazz, Stage stage) {
        this.primaryClass = clazz;
        this.primaryStage = stage;
        Scene rootScene = new Scene(new StackPane());
        primaryStage.setScene(rootScene);
    }

    public RouterConfig getConfig() {
        return config;
    }

    public void registerRoute(Route route) {
        routes.put(route.name(), route);
    }

    public void registerRoute(String name, String fxmlPath, Class<?> controllerClass) {
        routes.put(name, new Route(name, fxmlPath, controllerClass));
    }

    public void registerRoute(String name, String fxmlPath, Class<?> controllerClass, String transitionName) {
        routes.put(name, new Route(name, fxmlPath, controllerClass).withTransition(transitionName));
    }

    // Basic navigation
    public void to(String routeName) {
        to(routeName, null);
    }

    public void to(String routeName, Object arguments) {
        Route route = routes.get(routeName);
        if (route == null) throw new IllegalArgumentException("Route not registered: " + routeName);
        navigateTo(route, arguments, false);
    }

    // Replace current screen
    public void off(String routeName) {
        off(routeName, null);
    }

    public void off(String routeName, Object arguments) {
        Route route = routes.get(routeName);
        navigateTo(route, arguments, true);
    }

    // Clear all and start new
    public void offAll(String routeName) {
        navigationStack.clear();
        to(routeName);
    }

    // Go back
    public void back() {
        if (navigationStack.size() > 1) {
            navigationStack.pop();
            Route previous = navigationStack.peek();
            navigateTo(previous, null, false);
        }
    }

    private void navigateTo(Route route, Object arguments, boolean replace) {
        try {
            if (!runMiddlewares(route, arguments)) return;

            Parent root = FxControllerLoader.load(
                    primaryClass,
                    route.fxmlPath()
            );

            updateSceneRoot(root);

            applyTransition(root, route);

            handleControllerNavigation(route, arguments);

            updateNavigationStack(route, replace);

        } catch (Exception e) {
            throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
        }
    }

    private void applyTransition(Parent root, Route route) {
        TransitionEffect effect = config.getTransition(route.transitionName());
        if (effect != null) {
            effect.apply(root);
        }
    }

    private void updateSceneRoot(Parent root) {
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(root));
        } else {
            primaryStage.getScene().setRoot(root);
        }
    }

    private void handleControllerNavigation(Route route, Object arguments) {
        Object controller = ComponentStore.getComponent(route.controllerClass());
        if (controller instanceof Routable) {
            ((Routable) controller).onNavigate(arguments);
        }
    }

    private void updateNavigationStack(Route route, boolean replace) {
        if (!replace) {
            navigationStack.push(route);
        }
    }

    private boolean runMiddlewares(Route route, Object arguments) {
        Route current = navigationStack.isEmpty() ? null : navigationStack.peek();
        for (Middleware middleware : config.getMiddlewares()) {
            if (!middleware.handle(current, route, arguments)) {
                return false;
            }
        }
        return true;
    }
}