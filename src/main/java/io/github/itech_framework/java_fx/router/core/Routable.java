package io.github.itech_framework.java_fx.router.core;

public interface Routable {
    default void onNavigate(Object arguments) {}
    default void onReturn(Object result) {}
    default void onResume(){}
    /**
     * Called after FXML reload but before scene update
     */
    default void preRefresh() {}

    /**
     * Called after scene update completes
     */
    default void postRefresh() {}

    /**
     * Framework-managed refresh (final to prevent override)
     */
    default void refresh() {
        preRefresh();
        postRefresh();
    }
}
