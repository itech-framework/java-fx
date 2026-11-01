package io.github.itech_framework.java_fx.loader;

import java.util.function.Supplier;

interface ComponentRuntime {

    <T> T onUiThread(Supplier<T> action);

    <T> T load(Class<T> type, Object[] constructorArgs);

    void dispose(Object instance);
}