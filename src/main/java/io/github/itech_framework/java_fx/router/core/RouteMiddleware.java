package io.github.itech_framework.java_fx.router.core;

public abstract class RouteMiddleware {
    public abstract boolean handle(Route currentRoute, Route nextRoute);
}
