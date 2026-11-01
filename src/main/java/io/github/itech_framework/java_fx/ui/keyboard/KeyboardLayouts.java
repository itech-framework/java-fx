package io.github.itech_framework.java_fx.ui.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

public final class KeyboardLayouts {
    public static final String PROPERTY = "fx.keyboard.layout";
    public static final String DEFAULT_ID = "qwerty";

    private static final Map<String, KeyboardLayout> LAYOUTS = Collections.synchronizedMap(new LinkedHashMap<>());
    private static volatile KeyboardLayoutResolver resolver;
    private static boolean loaded;

    private KeyboardLayouts() {}

    public static synchronized void register(KeyboardLayout layout) {
        ensureLoaded();
        LAYOUTS.put(layout.id(), layout);
    }

    public static synchronized Optional<KeyboardLayout> get(String id) {
        ensureLoaded();
        return Optional.ofNullable(id == null ? null : LAYOUTS.get(id));
    }

    public static synchronized List<String> ids() {
        ensureLoaded();
        return new ArrayList<>(LAYOUTS.keySet());
    }

    public static void setResolver(KeyboardLayoutResolver resolver) {
        KeyboardLayouts.resolver = resolver;
    }

    public static void assign(Node node, String layoutId) {
        if (layoutId == null) {
            node.getProperties().remove(PROPERTY);
        } else {
            node.getProperties().put(PROPERTY, layoutId);
        }
    }

    public static KeyboardLayout resolve(TextInputControl input, String configuredDefault) {
        Object assigned = input.getProperties().get(PROPERTY);
        if (assigned instanceof String id) {
            Optional<KeyboardLayout> layout = get(id);
            if (layout.isPresent()) {
                return layout.get();
            }
        }
        KeyboardLayoutResolver current = resolver;
        if (current != null) {
            Optional<KeyboardLayout> layout = get(current.resolve(input));
            if (layout.isPresent()) {
                return layout.get();
            }
        }
        return get(configuredDefault).or(() -> get(DEFAULT_ID)).orElseThrow();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (KeyboardLayout layout : BuiltInLayouts.all()) {
            LAYOUTS.put(layout.id(), layout);
        }
        for (KeyboardLayoutProvider provider : ServiceLoader.load(KeyboardLayoutProvider.class)) {
            for (KeyboardLayout layout : provider.layouts()) {
                LAYOUTS.put(layout.id(), layout);
            }
        }
    }
}