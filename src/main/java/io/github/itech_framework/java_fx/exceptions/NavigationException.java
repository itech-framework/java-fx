package io.github.itech_framework.java_fx.exceptions;

import io.github.itech_framework.core.exceptions.FrameworkException;

public class NavigationException extends FrameworkException {
    private static final long serialVersionUID = 6741185226493382091L;

    private final String routeName;

    public NavigationException(String routeName, String message) {
        super(message);
        this.routeName = routeName;
    }

    public NavigationException(String routeName, Exception cause) {
        super("Navigation to '" + routeName + "' failed: " + rootMessage(cause), cause);
        this.routeName = routeName;
    }

    public String getRouteName() {
        return routeName;
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}