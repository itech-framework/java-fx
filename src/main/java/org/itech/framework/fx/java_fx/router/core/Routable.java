package org.itech.framework.fx.java_fx.router.core;

public interface Routable {
    default void onNavigate(Object arguments) {}
    default void onReturn(Object result) {}
}
