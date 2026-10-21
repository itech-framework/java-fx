package io.github.itech_framework.java_fx.router.config;

import io.github.itech_framework.java_fx.exceptions.NavigationException;
import io.github.itech_framework.java_fx.router.core.Route;

@FunctionalInterface
public interface NavigationErrorHandler {
    void handle(Route route, Object arguments, NavigationException error);
}