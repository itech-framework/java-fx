package io.github.itech_framework.java_fx.loader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import io.github.itech_framework.core.annotations.dependency.Use;
import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.annotations.FxUse;
import io.github.itech_framework.java_fx.annotations.components.FxComponent;
import io.github.itech_framework.java_fx.annotations.components.FxScope;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxThread;

public final class FxInjector {

    private static final ComponentRuntime FX_RUNTIME = new ComponentRuntime() {
        @Override
        public <T> T onUiThread(Supplier<T> action) {
            return FxThread.call(action);
        }

        @Override
        public <T> T load(Class<T> type, Object[] constructorArgs) {
            return FxComponentLoader.load(type, constructorArgs);
        }

        @Override
        public void dispose(Object instance) {
            FxComponentLoader.dispose(instance);
        }
    };

    private FxInjector() {
    }

    public static void inject(Object owner) {
        inject(owner, FX_RUNTIME, FxSettings.get().components().liveWarn());
    }

    static void inject(Object owner, ComponentRuntime runtime, int warnLimit) {
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                FxUse use = field.getAnnotation(FxUse.class);
                if (use != null) {
                    injectField(owner, field, use, runtime, warnLimit);
                }
            }
        }
    }

    private static void injectField(Object owner, Field field, FxUse use, ComponentRuntime runtime, int warnLimit) {
        String where = owner.getClass().getName() + "." + field.getName();
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
            throw new FrameworkException("@FxUse field " + where + " must not be static or final.");
        }
        if (field.isAnnotationPresent(Use.class)) {
            throw new FrameworkException("Field " + where + " has both @FxUse and @Use. Use only one of them.");
        }
        Object value;
        if (field.getType() == FxProvider.class) {
            Class<?> componentType = providerTypeOf(field, where);
            requireFxComponent(componentType, where);
            value = newProvider(owner, componentType, use.scope(), runtime, warnLimit);
        } else if (field.getType().isAnnotationPresent(FxComponent.class)) {
            if (use.scope() != FxScope.OWNER) {
                throw new FrameworkException("Field " + where + " uses scope " + use.scope()
                        + " but holds a component instance directly. Declare it as FxProvider<"
                        + field.getType().getSimpleName() + "> to create several instances.");
            }
            value = newProvider(owner, field.getType(), FxScope.OWNER, runtime, warnLimit).get();
        } else {
            throw new FrameworkException("@FxUse field " + where + " has type " + field.getType().getName()
                    + ". Use FxProvider<YourComponent> or a class annotated with @FxComponent.");
        }
        try {
            field.setAccessible(true);
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new FrameworkException("Cannot assign @FxUse field " + where, e);
        }
    }

    private static <T> OwnedProvider<T> newProvider(Object owner, Class<T> type, FxScope scope,
            ComponentRuntime runtime, int warnLimit) {
        OwnedProvider<T> provider = new OwnedProvider<>(type, scope, runtime, warnLimit);
        FxOwnership.register(owner, provider);
        return provider;
    }

    private static Class<?> providerTypeOf(Field field, String where) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized
                && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> componentType) {
            return componentType;
        }
        throw new FrameworkException("@FxUse field " + where
                + " must declare the component type, for example FxProvider<WelcomeWidget>.");
    }

    private static void requireFxComponent(Class<?> type, String where) {
        if (!type.isAnnotationPresent(FxComponent.class)) {
            throw new FrameworkException("@FxUse field " + where + " refers to " + type.getName()
                    + ", which is not annotated with @FxComponent.");
        }
    }
}