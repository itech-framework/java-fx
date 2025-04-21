package io.github.itech_framework.java_fx.router.config;

import io.github.itech_framework.java_fx.router.core.Route;

public interface Middleware {
    boolean handle(Route currentRoute, Route nextRoute, Object arguments);
}