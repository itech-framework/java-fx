package io.github.itech_framework.java_fx.router.core;

public class Route {
    private final String name;
    private final String fxmlPath;
    private final Class<?> controllerClass;
    private String transitionName;

    public Route(String name, String fxmlPath, Class<?> controllerClass) {
        this.name = name;
        this.fxmlPath = fxmlPath;
        this.controllerClass = controllerClass;
    }

    public Route withTransition(String transitionName) {
        this.transitionName = transitionName;
        return this;
    }

    public String transitionName() { return transitionName; }
    public String name() { return name; }
    public String fxmlPath() { return fxmlPath; }
    public Class<?> controllerClass() { return controllerClass; }
}