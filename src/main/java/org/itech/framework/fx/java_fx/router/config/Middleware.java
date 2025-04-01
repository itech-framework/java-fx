package org.itech.framework.fx.java_fx.router.config;

import org.itech.framework.fx.java_fx.router.core.Route;

public interface Middleware {
    boolean handle(Route currentRoute, Route nextRoute, Object arguments);
}