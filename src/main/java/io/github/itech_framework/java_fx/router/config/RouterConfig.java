package io.github.itech_framework.java_fx.router.config;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.itech_framework.java_fx.exceptions.DuplicateConfigKey;
import io.github.itech_framework.java_fx.ui.layout.LayoutConfig;

public class RouterConfig {
    @Deprecated
    private final List<Middleware> middlewares = new ArrayList<>();
    
    private final List<RouteMiddleware> routeMiddleWares = new ArrayList<RouteMiddleware>();
    
    private final Map<String, TransitionEffect> transitions = new HashMap<>();

    private static String darkModeKey = "isDarkMode";

    private final List<String> styleSheets = new ArrayList<>();

    private TransitionEffect defaultTransition = root -> {};
    
    private final Map<String, LayoutConfig> layoutConfigs = new HashMap<>();

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
        middlewares.add(middleware);
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
        routeMiddleWares.add(middleware);
    }

    public void addTransition(String name, TransitionEffect effect) {
    	if(transitions.containsKey(name)) throw new DuplicateConfigKey(name);
        transitions.put(name, effect);
    }
    
    public void addLayoutConfig(String name, LayoutConfig config) {
    	if(layoutConfigs.containsKey(name)) throw new DuplicateConfigKey(name);
    	layoutConfigs.put(name, config);
    }
    
    public LayoutConfig getLayout(String name) {
    	return layoutConfigs.getOrDefault(name, null);
    }

    public void addStyleSheets(String cssPath){
        styleSheets.add(cssPath);
    }

    public TransitionEffect getTransition(String name) {
        return transitions.getOrDefault(name, defaultTransition);
    }

	public static String getDarkModeKey() {
		return darkModeKey;
	}

	public static void setDarkModeKey(String darkModeKey) {
		RouterConfig.darkModeKey = darkModeKey;
	}

	public TransitionEffect getDefaultTransition() {
		return defaultTransition;
	}

	public void setDefaultTransition(TransitionEffect defaultTransition) {
		this.defaultTransition = defaultTransition;
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

}