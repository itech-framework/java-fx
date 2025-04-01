package org.itech.framework.fx.java_fx.router.config;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouterConfig {
    @Getter
    private final List<Middleware> middlewares = new ArrayList<>();
    private final Map<String, TransitionEffect> transitions = new HashMap<>();
    @Setter
    private TransitionEffect defaultTransition = root -> {};

    public void addMiddleware(Middleware middleware) {
        middlewares.add(middleware);
    }

    public void addTransition(String name, TransitionEffect effect) {
        transitions.put(name, effect);
    }

    public TransitionEffect getTransition(String name) {
        return transitions.getOrDefault(name, defaultTransition);
    }
}