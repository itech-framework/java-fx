package io.github.itech_framework.java_fx.router;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.core.utils.DataStorageUtil;
import io.github.itech_framework.core.utils.validator.CommonValidator;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.exceptions.DuplicateConfigKey;
import io.github.itech_framework.java_fx.exceptions.NavigationException;
import io.github.itech_framework.java_fx.loader.FxControllerLoader;
import io.github.itech_framework.java_fx.loader.FxControllerLoader.Result;
import io.github.itech_framework.java_fx.router.config.DisposePolicy;
import io.github.itech_framework.java_fx.router.config.Middleware;
import io.github.itech_framework.java_fx.router.config.NavigationErrorHandler;
import io.github.itech_framework.java_fx.router.config.RouteMiddleware;
import io.github.itech_framework.java_fx.router.config.RouterConfig;
import io.github.itech_framework.java_fx.router.config.TransitionEffect;
import io.github.itech_framework.java_fx.router.core.MiddlewareResult;
import io.github.itech_framework.java_fx.router.core.Routable;
import io.github.itech_framework.java_fx.router.core.Route;
import io.github.itech_framework.java_fx.ui.layout.LayoutConfig;
import io.github.itech_framework.java_fx.ui.layout.LayoutController;
import io.github.itech_framework.java_fx.utils.FxResources;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Router {
    private static final Logger logger = LogManager.getLogger(Router.class);

    private final Map<String, Route> routes = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Deque<NavigationState> navigationStack = new ArrayDeque<>();
    private final RouterConfig config = new RouterConfig();
    private Stage primaryStage;
    private Class<?> primaryClass;
    private Object currentArguments;
    private Route currentRoute;
    private Parent currentRoot;
    private int redirectDepth;

    public RouterConfig getConfig() {
        return config;
    }

    public Object getCurrentArguments() {
        return currentArguments;
    }

    public Route getCurrentRoute() {
        return currentRoute;
    }

    public void initialize(Class<?> clazz, Stage stage) {
        this.primaryClass = Objects.requireNonNull(clazz, "primary class must not be null");
        this.primaryStage = Objects.requireNonNull(stage, "primary stage must not be null");

        FxSettings settings = FxSettings.get();
        if (settings.theme().enabled()) {
            String themeCss = FxResources.themeStylesheet();
            if (!config.getStyleSheets().contains(themeCss)) {
                config.getStyleSheets().add(0, themeCss);
            }
        }
        primaryStage.setScene(new Scene(new StackPane()));

        if (settings.router().validateOnInit()) {
            validateRoutes();
        }
    }

    public void validateRoutes() {
        requireInitialized();
        List<String> problems = new ArrayList<>();
        List<Route> snapshot;
        synchronized (routes) {
            snapshot = new ArrayList<>(routes.values());
        }
        for (Route route : snapshot) {
            if (FxResources.find(primaryClass, route.fxmlPath()) == null) {
                problems.add("route '" + route.name() + "': FXML not found: " + route.fxmlPath());
            }
            if (CommonValidator.validString(route.layout())) {
                LayoutConfig layout = config.getLayout(route.layout());
                if (layout == null) {
                    problems.add("route '" + route.name() + "': layout '" + route.layout() + "' is not registered");
                } else if (layout.isEnabled() && FxResources.find(primaryClass, layout.getLayoutFxml()) == null) {
                    problems.add("route '" + route.name() + "': layout FXML not found: " + layout.getLayoutFxml());
                }
            }
            if (CommonValidator.validString(route.transitionName())
                    && !config.getTransitions().containsKey(route.transitionName())) {
                problems.add("route '" + route.name() + "': transition '" + route.transitionName()
                        + "' is not registered");
            }
        }
        if (!problems.isEmpty()) {
            throw new FrameworkException("Router configuration is invalid:\n - " + String.join("\n - ", problems));
        }
    }

    public void registerRoute(Route route) {
        Objects.requireNonNull(route, "route must not be null");
        if (routes.putIfAbsent(route.name(), route) != null) {
            throw new DuplicateConfigKey(route.name());
        }
    }

    public void replaceRoute(Route route) {
        Objects.requireNonNull(route, "route must not be null");
        routes.put(route.name(), route);
    }

    public void registerRoute(String name, String fxmlPath, Class<?> controllerClass) {
        registerRoute(name, fxmlPath, controllerClass, null, null);
    }

    public void registerRoute(String name, String fxmlPath, Class<?> controllerClass, String transitionName) {
        registerRoute(name, fxmlPath, controllerClass, transitionName, null);
    }

    public void registerRoute(String name, String fxmlPath, Class<?> controllerClass, String transitionName,
            String layout) {
        Route route = new Route(name, fxmlPath, controllerClass);
        if (CommonValidator.validString(transitionName)) {
            route = route.withTransition(transitionName);
        }
        if (CommonValidator.validString(layout)) {
            route = route.withLayout(layout);
        }
        registerRoute(route);
    }

    public boolean hasRoute(String name) {
        return routes.containsKey(name);
    }

    public Map<String, Route> getRoutes() {
        synchronized (routes) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(routes));
        }
    }

    public boolean canGoBack() {
        return navigationStack.size() > 1;
    }

    public void to(String routeName) {
        to(routeName, null);
    }

    public void to(String routeName, Object arguments) {
        navigate(routeName, arguments, Mode.PUSH);
    }

    public void off(String routeName) {
        off(routeName, null);
    }

    public void off(String routeName, Object arguments) {
        navigate(routeName, arguments, Mode.REPLACE);
    }

    public void offAll(String routeName) {
        offAll(routeName, null);
    }

    public void offAll(String routeName, Object arguments) {
        navigate(routeName, arguments, Mode.RESET);
    }

    public void back() {
        runOnFxThread(this::goBack);
    }

    public void pop() {
        runOnFxThread(() -> {
            if (navigationStack.size() < 2) {
                return;
            }
            Iterator<NavigationState> states = navigationStack.iterator();
            states.next();
            NavigationState previous = states.next();
            navigateTo(previous.route(), previous.arguments(), 2, false);
        });
    }

    public void refresh() {
        refresh(currentArguments);
    }

    public void refresh(Object newArguments) {
        runOnFxThread(() -> doRefresh(newArguments));
    }

    public void refreshTheme() {
        runOnFxThread(() -> {
            if (primaryStage != null && primaryStage.getScene() != null) {
                applyTheme(primaryStage.getScene());
            }
        });
    }

    public void shutdown() {
        while (!navigationStack.isEmpty()) {
            navigationStack.pop().remove();
        }
        currentRoute = null;
        currentRoot = null;
        currentArguments = null;
    }

    private void navigate(String routeName, Object arguments, Mode mode) {
        Route route = requireRoute(routeName);
        runOnFxThread(() -> {
            int removeCount = switch (mode) {
                case PUSH -> 0;
                case REPLACE -> Math.min(1, navigationStack.size());
                case RESET -> navigationStack.size();
            };
            navigateTo(route, arguments, removeCount, mode == Mode.PUSH);
        });
    }

    private void navigateTo(Route route, Object arguments, int removeCount, boolean leaveCurrent) {
        requireInitialized();
        logger.debug("Navigating to {}", route.name());
        try {
            if (!passesMiddlewares(route, arguments)) {
                return;
            }
            LoadResult loaded = loadWithLayout(route);

            NavigationState previous = navigationStack.peek();
            if (leaveCurrent && previous != null) {
                previous.leave(FxSettings.get().router().disposePolicy());
            }
            for (int i = 0; i < removeCount && !navigationStack.isEmpty(); i++) {
                navigationStack.pop().remove();
            }

            NavigationState state = new NavigationState(route, loaded.root(), arguments, loaded.controller(),
                    loaded.layoutController());
            navigationStack.push(state);
            show(state);
            if (state.controller() instanceof Routable routable) {
                routable.onNavigate(arguments);
            }
            logger.debug("Navigated to {}", route.name());
        } catch (Exception e) {
            fail(route, arguments, e);
        }
    }

    private void goBack() {
        if (navigationStack.size() <= 1) {
            return;
        }
        NavigationState previous = null;
        try {
            navigationStack.pop().remove();
            previous = navigationStack.peek();
            previous.resume();
            show(previous);
        } catch (Exception e) {
            fail(previous != null ? previous.route() : currentRoute, currentArguments, e);
        }
    }

    private void doRefresh(Object newArguments) {
        NavigationState current = navigationStack.peek();
        if (current == null) {
            return;
        }
        try {
            LoadResult loaded = loadWithLayout(current.route());
            Object controller = loaded.controller();
            if (controller instanceof Routable routable) {
                routable.onNavigate(newArguments);
                routable.preRefresh();
            }
            if (current.controller() instanceof Routable old) {
                old.onReturn(null);
            }
            navigationStack.pop().remove();

            NavigationState fresh = new NavigationState(current.route(), loaded.root(), newArguments, controller,
                    loaded.layoutController());
            navigationStack.push(fresh);
            show(fresh);
            if (controller instanceof Routable routable) {
                routable.postRefresh();
            }
        } catch (Exception e) {
            fail(current.route(), newArguments, new NavigationException(current.route().name(), e));
        }
    }

    private LoadResult loadWithLayout(Route route) throws Exception {
        String layoutName = route.layout();
        LayoutConfig layout = null;
        if (CommonValidator.validString(layoutName)) {
            layout = config.getLayout(layoutName);
            if (layout == null) {
                throw new NavigationException(route.name(),
                        "Layout not found: '" + layoutName + "'. Registered layouts: " + config.getLayouts().keySet());
            }
        }

        if (layout == null || !layout.isEnabled()) {
            Result<Parent, Object> result = FxControllerLoader.loadWithResult(primaryClass, route.fxmlPath());
            return new LoadResult(result.getRoot(), result.getController(), null);
        }

        Result<Parent, Object> layoutResult = FxControllerLoader.loadWithResult(primaryClass, layout.getLayoutFxml());
        Result<Parent, Object> contentResult;
        try {
            contentResult = FxControllerLoader.loadWithResult(primaryClass, route.fxmlPath());
        } catch (Exception | Error e) {
            destroyQuietly(layoutResult.getController());
            throw e;
        }

        Parent layoutRoot = layoutResult.getRoot();
        Object layoutController = layoutResult.getController();
        try {
            if (layoutController instanceof LayoutController layoutCtl) {
                layoutCtl.setContent(contentResult.getRoot());
                layoutCtl.onLayoutLoaded();
            } else {
                Node container = layoutRoot.lookup("#" + layout.getContentContainerId());
                if (container instanceof Pane pane) {
                    pane.getChildren().add(contentResult.getRoot());
                } else {
                    throw new NavigationException(route.name(), "Layout container '#" + layout.getContentContainerId()
                            + "' was not found or is not a Pane in " + layout.getLayoutFxml());
                }
            }
        } catch (Exception | Error e) {
            destroyQuietly(contentResult.getController());
            destroyQuietly(layoutController);
            throw e;
        }
        return new LoadResult(layoutRoot, contentResult.getController(), layoutController);
    }

    private void show(NavigationState state) {
        updateSceneRoot(state.root());
        applyTransition(state.root(), state.route());
        currentArguments = state.arguments();
        currentRoot = state.root();
        currentRoute = state.route();
    }

    private void applyTransition(Parent root, Route route) {
        TransitionEffect effect = config.getTransition(route.transitionName());
        if (effect != null) {
            effect.apply(root);
        }
    }

    private void updateSceneRoot(Parent root) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            scene = new Scene(root);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        ObservableList<String> sheets = scene.getStylesheets();
        for (String css : config.getStyleSheets()) {
            if (!sheets.contains(css)) {
                sheets.add(css);
            }
        }
        applyTheme(scene);
    }

    private void applyTheme(Scene scene) {
        FxSettings.Theme theme = FxSettings.get().theme();
        Object stored = DataStorageUtil.load(theme.darkModeKey());
        if (stored == null) {
            return;
        }
        boolean dark = Boolean.parseBoolean(String.valueOf(stored).replace("\"", ""));
        ObservableList<String> styleClasses = scene.getRoot().getStyleClass();
        if (dark) {
            if (!styleClasses.contains(theme.darkStyleClass())) {
                styleClasses.add(theme.darkStyleClass());
            }
            scene.setFill(theme.darkFill());
        } else {
            styleClasses.remove(theme.darkStyleClass());
            scene.setFill(theme.lightFill());
        }
    }

    private boolean passesMiddlewares(Route route, Object arguments) {
        NavigationState top = navigationStack.peek();
        Route current = top == null ? null : top.route();

        for (Middleware middleware : config.getMiddlewares()) {
            if (!middleware.handle(current, route, arguments)) {
                return false;
            }
        }
        for (RouteMiddleware middleware : config.getRouteMiddleWares()) {
            MiddlewareResult result = middleware.handle(current, route, arguments);
            if (result == null) {
                throw new NavigationException(route.name(), "A route middleware returned null. "
                        + "Return MiddlewareResult.proceed(), abort() or redirect(...).");
            }
            if (!result.shouldProceed()) {
                handleMiddlewareFailure(route, result);
                return false;
            }
        }
        return true;
    }

    private void handleMiddlewareFailure(Route route, MiddlewareResult result) {
        logger.debug("Middleware blocked navigation to {}", route.name());
        if (result.getRedirectTo() != null) {
            int max = FxSettings.get().router().maxRedirects();
            if (redirectDepth >= max) {
                throw new NavigationException(route.name(), "More than " + max
                        + " middleware redirects in a row (last target: '" + result.getRedirectTo()
                        + "'). Check your middlewares for a redirect loop or raise fx.router.max-redirects.");
            }
            redirectDepth++;
            try {
                to(result.getRedirectTo(), result.getRedirectArguments());
            } finally {
                redirectDepth--;
            }
            return;
        }
        if (result.getOnFailureCallback() != null) {
            result.getOnFailureCallback().run();
        }
    }

    private void fail(Route route, Object arguments, Exception cause) {
        String routeName = route == null ? "?" : route.name();
        boolean alreadyReported = cause instanceof NavigationException;
        NavigationException error = alreadyReported ? (NavigationException) cause
                : new NavigationException(routeName, cause);
        if (!alreadyReported) {
            logger.error("Navigation to '{}' failed", routeName, cause);
        }

        NavigationErrorHandler handler = config.getErrorHandler();
        if (handler != null) {
            try {
                handler.handle(route, arguments, error);
                return;
            } catch (RuntimeException handlerFailure) {
                logger.error("The navigation error handler failed", handlerFailure);
            }
        }
        throw error;
    }

    private Route requireRoute(String routeName) {
        Route route = routes.get(routeName);
        if (route == null) {
            throw new IllegalArgumentException(
                    "Route not registered: " + routeName + ". Registered routes: " + getRoutes().keySet());
        }
        return route;
    }

    private void requireInitialized() {
        if (primaryStage == null || primaryClass == null) {
            throw new FrameworkException("Router is not initialized. Call router.initialize(MainClass.class, stage) "
                    + "in start() before navigating.");
        }
    }

    private void runOnFxThread(Runnable action) {
        if (!FxSettings.get().router().runOnFxThread() || Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static void destroyQuietly(Object controller) {
        if (controller == null) {
            return;
        }
        try {
            ComponentProcessor.destroyInstance(controller.getClass(), controller);
        } catch (RuntimeException e) {
            logger.error("Error destroying {}", controller.getClass().getSimpleName(), e);
        }
    }

    private enum Mode {
        PUSH, REPLACE, RESET
    }

    private record LoadResult(Parent root, Object controller, Object layoutController) {
    }

    private static final class NavigationState {
        private final Route route;
        private final Parent root;
        private final Object arguments;
        private final Object controller;
        private final Object layoutController;
        private boolean disposed;
        private boolean destroyed;

        NavigationState(Route route, Parent root, Object arguments, Object controller, Object layoutController) {
            this.route = route;
            this.root = root;
            this.arguments = arguments;
            this.controller = controller;
            this.layoutController = layoutController;
        }

        Route route() {
            return route;
        }

        Parent root() {
            return root;
        }

        Object arguments() {
            return arguments;
        }

        Object controller() {
            return controller;
        }

        void leave(DisposePolicy policy) {
            if (policy == DisposePolicy.ON_LEAVE) {
                dispose();
            }
        }

        void resume() {
            disposed = false;
            if (controller instanceof Routable routable) {
                routable.onResume();
            }
        }

        void remove() {
            dispose();
            destroy();
        }

        private void dispose() {
            if (disposed) {
                return;
            }
            disposed = true;
            disposeQuietly(controller);
            disposeQuietly(layoutController);
        }

        private void destroy() {
            if (destroyed) {
                return;
            }
            destroyed = true;
            destroyQuietly(controller);
            destroyQuietly(layoutController);
        }

        private static void disposeQuietly(Object target) {
            if (target instanceof Routable routable) {
                try {
                    logger.debug("Disposing controller: {}", target.getClass().getSimpleName());
                    routable.dispose();
                } catch (RuntimeException e) {
                    logger.error("Error disposing controller {}", target.getClass().getSimpleName(), e);
                }
            }
        }
    }

	public Parent getCurrentRoot() {
		return currentRoot;
	}

	public void setCurrentRoot(Parent currentRoot) {
		this.currentRoot = currentRoot;
	}
    
}