package io.github.itech_framework.java_fx.router.config;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.exceptions.DuplicateConfigKey;
import io.github.itech_framework.java_fx.ui.layout.LayoutConfig;

public class RouterConfig {
    @Deprecated
    private final List<Middleware> middlewares = new ArrayList<>();
    
    private final List<RouteMiddleware> routeMiddleWares = new ArrayList<RouteMiddleware>();
    
    private final Map<String, TransitionEffect> transitions = new HashMap<>();

    private NavigationErrorHandler errorHandler;

    private final List<String> styleSheets = new ArrayList<>();

    private TransitionEffect defaultTransition = root -> {};
    
    private final Map<String, LayoutConfig> layoutConfigs = new LinkedHashMap<>();

    /**
     * Adds a legacy middleware to the router configuration.
     * 
     * @deprecated Since version 1.0.11. Use {@link #addRouteMiddleware(RouteMiddleware)} instead 
     * for enhanced middleware functionality with redirects and callbacks. This method will 
     * be removed in a future version.
     * 
     * @param middleware The legacy middleware that returns a simple boolean to allow or 
     *                   block navigation. When returning false, navigation is silently 
     *                   blocked without any additional actions.
     * 
     * @throws NullPointerException if the provided middleware is null
     * 
     * @example
     * // Legacy usage (deprecated)
     * router.getConfig().addMiddleware((currentRoute, nextRoute, arguments) -> {
     *     if (nextRoute.name().equals("dashboard")) {
     *         return isUserLoggedin; // Simply return true/false
     *     }
     *     return true;
     * });
     * 
     * @see RouteMiddleware
     * @see #addRouteMiddleware(RouteMiddleware)
     */
    @Deprecated(since = "1.0.11", forRemoval = true)
    public void addMiddleware(Middleware middleware) {
        middlewares.add(Objects.requireNonNull(middleware, "middleware must not be null"));
    }

    /**
     * Adds an advanced middleware to the router configuration with enhanced capabilities
     * including redirects, callbacks, and more granular control over navigation flow.
     * 
     * <p>This method replaces the legacy {@link #addMiddleware(Middleware)} method and provides
     * the following advanced features:</p>
     * 
     * <ul>
     *   <li><b>Redirects</b>: Automatically redirect to another route when navigation is blocked</li>
     *   <li><b>Callbacks</b>: Execute custom logic when navigation is blocked</li>
     *   <li><b>Flexible flow control</b>: More sophisticated navigation decision making</li>
     * </ul>
     * 
     * <p>Middlewares run in the order they were added. The first one that does not return
     * {@code MiddlewareResult.proceed()} stops the chain. A middleware that returns {@code null}
     * fails the navigation with a {@code NavigationException}, and a chain of redirects longer
     * than {@code fx.router.max-redirects} is reported as a redirect loop.</p>
     * 
     * @param middleware The advanced middleware that returns a {@link MiddlewareResult} 
     *                   to control navigation behavior with options to proceed, abort, 
     *                   redirect, or execute callbacks.
     * 
     * @example
     * // Simple boolean check (equivalent to legacy behavior)
     * router.getConfig().addRouteMiddleware((currentRoute, nextRoute, arguments) -> {
     *     if (nextRoute.name().equals("dashboard")) {
     *         return isUserLoggedin ? MiddlewareResult.proceed() : MiddlewareResult.abort();
     *     }
     *     return MiddlewareResult.proceed();
     * });
     * 
     * @example
     * // Redirect when unauthorized
     * router.getConfig().addRouteMiddleware((currentRoute, nextRoute, arguments) -> {
     *     if (nextRoute.name().equals("admin") && !isUserAdmin) {
     *         return MiddlewareResult.redirect("login", "Admin access required");
     *     }
     *     return MiddlewareResult.proceed();
     * });
     * 
     * @example
     * // Execute callback with custom logic
     * router.getConfig().addRouteMiddleware((currentRoute, nextRoute, arguments) -> {
     *     if (nextRoute.name().equals("premium") && !hasPremiumAccess) {
     *         return MiddlewareResult.abortWithCallback(() -> {
     *             showUpgradeDialog();
     *             playAccessDeniedSound();
     *         });
     *     }
     *     return MiddlewareResult.proceed();
     * });
     * 
     * @throws NullPointerException if the provided middleware is null
     * 
     * @see MiddlewareResult
     * @see RouteMiddleware
     * @since 1.0.11
     */
    public void addRouteMiddleware(RouteMiddleware middleware) {
        routeMiddleWares.add(Objects.requireNonNull(middleware, "middleware must not be null"));
    }

    /**
     * Registers a named transition effect that routes can use when their page is shown.
     * 
     * <p>A route selects a transition by name, for example
     * {@code router.registerRoute("tasks", "/views/tasks.fxml", TasksController.class, "fade")}.
     * Transition names are checked when {@code router.initialize(...)} runs
     * (see {@code fx.router.validate-on-init}), so a typo in a route is reported at startup
     * instead of being silently ignored.</p>
     * 
     * @param name   The unique name of the transition. Must not be blank.
     * @param effect The effect that is applied to the root node of the new page. Must not be null.
     * 
     * @throws IllegalArgumentException if the name is null or blank
     * @throws NullPointerException     if the effect is null
     * @throws DuplicateConfigKey       if a transition with the same name is already registered
     * 
     * @example
     * router.getConfig().addTransition("fade", root -> {
     *     FadeTransition fade = new FadeTransition(Duration.millis(250), root);
     *     fade.setFromValue(0);
     *     fade.setToValue(1);
     *     fade.play();
     * });
     * 
     * @see #setDefaultTransition(TransitionEffect)
     * @see #getTransition(String)
     */
    public void addTransition(String name, TransitionEffect effect) {
        requireName(name, "transition");
        Objects.requireNonNull(effect, "transition effect must not be null");
        if (transitions.containsKey(name)) throw new DuplicateConfigKey(name);
        transitions.put(name, effect);
    }

    /**
     * Registers a named layout that routes can be rendered inside.
     * 
     * <p>A layout is an FXML shell (for example a window with a sidebar and a header) with a
     * container where the page content is placed. Routes choose a layout by name, and the layout
     * FXML is checked when {@code router.initialize(...)} runs.</p>
     * 
     * @param name   The unique name of the layout. Must not be blank.
     * @param config The layout definition: the FXML path and the id of the content container.
     *               Must not be null.
     * 
     * @throws IllegalArgumentException if the name is null or blank
     * @throws NullPointerException     if the config is null
     * @throws DuplicateConfigKey       if a layout with the same name is already registered
     * 
     * @example
     * router.getConfig().addLayoutConfig("main", new LayoutConfig("/views/layout/main.fxml"));
     * router.registerRoute("dashboard", "/views/dashboard.fxml", DashboardController.class, null, "main");
     * 
     * @see LayoutConfig
     * @see #getLayout(String)
     */
    public void addLayoutConfig(String name, LayoutConfig config) {
        requireName(name, "layout");
        Objects.requireNonNull(config, "layout config must not be null");
        if (layoutConfigs.containsKey(name)) throw new DuplicateConfigKey(name);
        layoutConfigs.put(name, config);
    }

    /**
     * Returns the layout registered under the given name.
     * 
     * @param name The layout name.
     * @return The layout configuration, or {@code null} if no layout is registered with that name.
     * 
     * @see #addLayoutConfig(String, LayoutConfig)
     */
    public LayoutConfig getLayout(String name) {
        return layoutConfigs.get(name);
    }

    /**
     * Returns all registered layouts in the order they were added.
     * 
     * @return An unmodifiable view of the registered layouts, keyed by layout name.
     * 
     * @see #addLayoutConfig(String, LayoutConfig)
     */
    public Map<String, LayoutConfig> getLayouts() {
        return Collections.unmodifiableMap(layoutConfigs);
    }

    /**
     * Adds an application stylesheet that the router applies to the scene on every navigation.
     * 
     * <p>Stylesheets are applied in the order they were added, and later ones override earlier
     * ones. The built-in theme (see {@code fx.theme.stylesheet}) is always placed first, so the
     * stylesheets you add here override it. Adding the same path twice has no effect.</p>
     * 
     * @param cssPath The external form of the stylesheet URL. Must not be blank.
     * 
     * @throws IllegalArgumentException if the path is null or blank, which usually means
     *                                  {@code getResource(...)} did not find the file
     * 
     * @example
     * router.getConfig().addStyleSheets(
     *         Objects.requireNonNull(getClass().getResource("/static/css/app.css")).toExternalForm());
     */
    public void addStyleSheets(String cssPath){
        if (cssPath == null || cssPath.isBlank()) {
            throw new IllegalArgumentException("Stylesheet path must not be blank. "
                    + "Pass getClass().getResource(\"/path.css\").toExternalForm() and check it is not null.");
        }
        if (!styleSheets.contains(cssPath)) {
            styleSheets.add(cssPath);
        }
    }

    /**
     * Returns the transition effect to use for the given transition name.
     * 
     * @param name The transition name used by a route. May be {@code null} for routes without one.
     * @return The registered effect, or the default transition if the name is {@code null}
     *         or not registered.
     * 
     * @see #addTransition(String, TransitionEffect)
     * @see #setDefaultTransition(TransitionEffect)
     */
    public TransitionEffect getTransition(String name) {
        return name == null ? defaultTransition : transitions.getOrDefault(name, defaultTransition);
    }

    /**
     * Returns the storage key that holds the dark mode flag.
     * 
     * <p>The router reads this key through {@code DataStorageUtil} on every navigation and
     * adds or removes the dark style class on the scene root. The value comes from the
     * {@code fx.theme.dark-mode-key} setting and defaults to {@code isDarkMode}.</p>
     * 
     * @return The storage key of the dark mode flag.
     * 
     * @see #setDarkModeKey(String)
     */
    public static String getDarkModeKey() {
        return FxSettings.get().theme().darkModeKey();
    }

    /**
     * Changes the storage key that holds the dark mode flag.
     * 
     * <p>This is the same as setting {@code fx.theme.dark-mode-key} in the properties file.
     * Call it before the application starts, so the preloader and the router read the same key.</p>
     * 
     * @param darkModeKey The new storage key.
     * 
     * @example
     * RouterConfig.setDarkModeKey("isDarkMode");
     * DataStorageUtil.save("isDarkMode", true);
     * router.refreshTheme();
     * 
     * @see #getDarkModeKey()
     */
    public static void setDarkModeKey(String darkModeKey) {
        FxSettings.override("fx.theme.dark-mode-key", darkModeKey);
    }

    /**
     * Returns the handler that is called when a navigation fails.
     * 
     * @return The error handler, or {@code null} if none is set.
     * 
     * @see #setErrorHandler(NavigationErrorHandler)
     */
    public NavigationErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the handler that is called when a navigation fails, for example when an FXML file
     * cannot be loaded, a controller is not registered or a middleware redirects in a loop.
     * 
     * <p>When a handler is set, the failure is passed to it and no exception is thrown to the
     * caller of {@code router.to(...)}. The page that was on screen stays in place, because the
     * new page is loaded before the old one is disposed. Without a handler, a
     * {@code NavigationException} is thrown. If the handler itself throws, the original
     * {@code NavigationException} is thrown instead.</p>
     * 
     * @param errorHandler The handler, or {@code null} to remove it.
     * 
     * @example
     * router.getConfig().setErrorHandler((route, arguments, error) -> {
     *     AlertDialog.error("Could not open " + route.name(), error.getMessage());
     * });
     * 
     * @see NavigationErrorHandler
     */
    public void setErrorHandler(NavigationErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Sets the transition used by routes that have no transition name, or whose name is not
     * registered.
     * 
     * @param defaultTransition The default effect, or {@code null} for no effect.
     * 
     * @see #addTransition(String, TransitionEffect)
     */
    public void setDefaultTransition(TransitionEffect defaultTransition) {
        this.defaultTransition = defaultTransition == null ? root -> {} : defaultTransition;
    }

    public TransitionEffect getDefaultTransition() {
        return defaultTransition;
    }

    public List<Middleware> getMiddlewares() {
        return middlewares;
    }

    public List<RouteMiddleware> getRouteMiddleWares() {
        return routeMiddleWares;
    }

    public Map<String, TransitionEffect> getTransitions() {
        return transitions;
    }

    public List<String> getStyleSheets() {
        return styleSheets;
    }

    /**
     * Rejects a null or blank name before it is used as a registry key.
     * 
     * @param name The name to check.
     * @param what What the name identifies, used in the error message (for example "layout").
     * 
     * @throws IllegalArgumentException if the name is null or blank
     */
    private static void requireName(String name, String what) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The " + what + " name must not be blank");
        }
    }

}