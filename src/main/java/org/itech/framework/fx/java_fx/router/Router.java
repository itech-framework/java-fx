package org.itech.framework.fx.java_fx.router;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.itech.framework.fx.core.store.ComponentStore;
import org.itech.framework.fx.core.utils.DataStorageUtil;
import org.itech.framework.fx.java_fx.helpers.FxControllerLoader;
import org.itech.framework.fx.java_fx.router.config.Middleware;
import org.itech.framework.fx.java_fx.router.config.RouterConfig;
import org.itech.framework.fx.java_fx.router.config.TransitionEffect;
import org.itech.framework.fx.java_fx.router.core.Routable;
import org.itech.framework.fx.java_fx.router.core.Route;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class Router {
    private final Map<String, Route> routes = new HashMap<>();
    private final Stack<NavigationState> navigationStack = new Stack<>();
    @Getter
    private final RouterConfig config = new RouterConfig();
    private Stage primaryStage;
    private Class<?> primaryClass;
    private Object currentArguments;
    private Route currentRoute;
    private Parent currentRoot;

    private static final Logger logger = LogManager.getLogger(Router.class);

    public void initialize(Class<?> clazz, Stage stage) {
        this.primaryClass = clazz;
        this.primaryStage = stage;
        Scene rootScene = new Scene(new StackPane());
        config.addStyleSheets(Objects.requireNonNull(getClass().getResource("/static/css/style.css")).toExternalForm());
        primaryStage.setScene(rootScene);
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
            NavigationState currentState = navigationStack.pop();
            NavigationState previousState = navigationStack.peek();

            // Notify the current controller it's being left
            if (currentState.controller() instanceof Routable) {
                ((Routable) currentState.controller()).onReturn(previousState.arguments());
            }

            // Update scene to previous state's root
            updateSceneRoot(previousState.root());
            applyTransition(previousState.root(), previousState.route());

            // Update current references
            currentArguments = previousState.arguments();
            currentRoot = previousState.root();
            currentRoute = previousState.route();

            // Notify the previous controller it's being resumed
            if (previousState.controller() instanceof Routable) {
                ((Routable) previousState.controller()).onResume();
            }
        }
    }

    public void pop(){
        if (navigationStack.size() > 1) {
            NavigationState currentState = navigationStack.pop();
            NavigationState previousState = navigationStack.peek();

            navigateTo(previousState.route, previousState.arguments, false);
        }
    }

    private void navigateTo(Route route, Object arguments, boolean replace) {
        logger.debug("Navigating to {}", route.name());
        try {
            if (!runMiddlewares(route, arguments)) return;

            logger.debug("Loading page...");

            Parent root = FxControllerLoader.load(
                    primaryClass,
                    route.fxmlPath()
            );
            Object controller = ComponentStore.getComponent(route.controllerClass());

            logger.debug("Page loaded.");

            updateSceneRoot(root);
            applyTransition(root, route);

            handleControllerNavigation(route, arguments, controller);

            // Update navigation stack
            if (replace && !navigationStack.isEmpty()) {
                navigationStack.pop();
            }

            navigationStack.push(new NavigationState(route, root, arguments, controller));

            currentArguments = arguments;
            currentRoot = root;
            currentRoute = route;
            logger.debug("Navigated to {}", route.name());
        } catch (Exception e) {
            throw new RuntimeException("Navigation failed: " + e.getMessage(), e);
        }
    }

    public void refresh() {
        refresh(currentArguments);
    }

    public void refresh(Object newArguments) {
        if (currentRoute == null) return;
        try {
            Parent newRoot = FxControllerLoader.load(
                    primaryClass,
                    currentRoute.fxmlPath()
            );

            Object newController = ComponentStore.getComponent(currentRoute.controllerClass());

            handleRefreshLifecycle(newController, newArguments);

            // Update view
            updateSceneRoot(newRoot);
            applyTransition(newRoot, currentRoute);

            // Update references
            currentRoot = newRoot;
            currentArguments = newArguments;

        } catch (Exception e) {
            throw new RuntimeException("Refresh failed", e);
        }
    }

    private void handleRefreshLifecycle(Object controller, Object arguments) {
        if (currentRoot != null) {
            Object oldController = ComponentStore.getComponent(currentRoute.controllerClass());
            if (oldController instanceof Routable) {
                ((Routable) oldController).onReturn(null);
            }
        }

        if (controller instanceof Routable routable) {
            routable.onNavigate(arguments);
            routable.refresh();
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
        primaryStage.getScene().getStylesheets().addAll(getConfig().getStyleSheets());
        // check for dark mode
        if(DataStorageUtil.load(RouterConfig.getDarkModeKey())!=null){
            boolean isDarkMode = Boolean.parseBoolean((String) DataStorageUtil.load(RouterConfig.getDarkModeKey()));
            if(isDarkMode){
                primaryStage.getScene().getRoot().getStyleClass().add("dark-mode");
                primaryStage.getScene().setFill(Color.BLACK);
            }else{
                primaryStage.getScene().getRoot().getStyleClass().remove("dark-mode");
                primaryStage.getScene().setFill(Color.WHITE);
            }
        }
    }

    private void handleControllerNavigation(Route route, Object arguments, Object controller) {
        if (controller instanceof Routable) {
            ((Routable) controller).onNavigate(arguments);
        }
    }

    /*private void updateNavigationStack(Route route, boolean replace) {
        if (!replace) {
            navigationStack.push(route);
        }
    }*/

    private boolean runMiddlewares(Route route, Object arguments) {
        Route current = navigationStack.isEmpty() ? null : navigationStack.peek().route;
        for (Middleware middleware : config.getMiddlewares()) {
            if (!middleware.handle(current, route, arguments)) {
                return false;
            }
        }
        return true;
    }

    private record NavigationState(Route route, Parent root, Object arguments, Object controller) {
    }
}