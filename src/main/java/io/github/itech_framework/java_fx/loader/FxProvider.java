package io.github.itech_framework.java_fx.loader;

public interface FxProvider<T> {

    T get();

    T create(Object... constructorArgs);

    void release(T instance);

    int liveCount();
}