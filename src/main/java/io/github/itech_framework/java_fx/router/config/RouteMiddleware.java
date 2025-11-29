package io.github.itech_framework.java_fx.router.config;

import io.github.itech_framework.java_fx.router.core.MiddlewareResult;
import io.github.itech_framework.java_fx.router.core.Route;

@FunctionalInterface
public interface RouteMiddleware {
	MiddlewareResult handle(Route currentRoute, Route nextRoute, Object arguments);
}
