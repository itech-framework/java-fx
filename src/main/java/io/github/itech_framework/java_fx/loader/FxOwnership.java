package io.github.itech_framework.java_fx.loader;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class FxOwnership {
    private static final Map<Object, List<OwnedProvider<?>>> PROVIDERS = new IdentityHashMap<>();

    private FxOwnership() {
    }

    static void register(Object owner, OwnedProvider<?> provider) {
        synchronized (PROVIDERS) {
            PROVIDERS.computeIfAbsent(owner, key -> new ArrayList<>()).add(provider);
        }
    }

    public static void disposeChildren(Object owner) {
        if (owner == null) {
            return;
        }
        List<OwnedProvider<?>> providers;
        synchronized (PROVIDERS) {
            providers = PROVIDERS.remove(owner);
        }
        if (providers == null) {
            return;
        }
        for (int i = providers.size() - 1; i >= 0; i--) {
            providers.get(i).disposeAll();
        }
    }

    public static void disposeAll() {
        List<Object> owners;
        synchronized (PROVIDERS) {
            owners = new ArrayList<>(PROVIDERS.keySet());
        }
        for (Object owner : owners) {
            disposeChildren(owner);
        }
    }

    public static int ownerCount() {
        synchronized (PROVIDERS) {
            return PROVIDERS.size();
        }
    }
}