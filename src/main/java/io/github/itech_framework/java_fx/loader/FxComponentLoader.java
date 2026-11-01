package io.github.itech_framework.java_fx.loader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.java_fx.annotations.components.FxComponent;
import io.github.itech_framework.java_fx.annotations.components.Root;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.FxResources;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public final class FxComponentLoader {
	private static final Map<Class<?>, Class<?>> WRAPPERS = Map.of(int.class, Integer.class, long.class, Long.class,
			double.class, Double.class, float.class, Float.class, boolean.class, Boolean.class, char.class,
			Character.class, byte.class, Byte.class, short.class, Short.class);

	private FxComponentLoader() {
	}

	public static <T> T load(Class<T> clazz, Object... constructorArgs) {
		FxComponent annotation = clazz.getAnnotation(FxComponent.class);
		if (annotation == null) {
			throw new FrameworkException("Class " + clazz.getName()
					+ " must be annotated with @FxComponent to be loaded by FxComponentLoader");
		}
		T instance = null;
		try {
			Constructor<T> constructor = findMatchingConstructor(clazz, constructorArgs);
			constructor.setAccessible(true);
			instance = constructor.newInstance(constructorArgs);

			String fxmlPath = annotation.value().isEmpty()
					? FxSettings.get().components().fxmlBase() + clazz.getSimpleName() + ".fxml"
					: annotation.value();
			URL location = FxResources.require(clazz, fxmlPath, "FXML view of @FxComponent " + clazz.getSimpleName());

			FXMLLoader loader = new FXMLLoader(location);
			loader.setController(instance);
			ComponentProcessor.injectFields(clazz, instance);

			FxInjector.inject(instance);

			Parent fxmlRoot = loader.load();

			injectRootAnnotatedField(instance, fxmlRoot);
			ComponentProcessor.runInitMethods(clazz, instance);
			return instance;
		} catch (Exception e) {
			FxOwnership.disposeChildren(instance);
			if (e instanceof FrameworkException frameworkException) {
				throw frameworkException;
			}
			throw new FrameworkException("Failed to create and initialize FXML class: " + clazz.getName(), e);
		}
	}

	public static void dispose(Object component) {
		if (component != null) {
            try {
                ComponentProcessor.destroyInstance(component.getClass(), component);
            } finally {
                FxOwnership.disposeChildren(component);
            }
		}
	}

	private static <T> void injectRootAnnotatedField(T controller, Parent root) {
		Field rootField = null;
		for (Class<?> type = controller.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				if (!field.isAnnotationPresent(Root.class)) {
					continue;
				}
				if (rootField != null) {
					throw new FrameworkException(
							"Multiple fields annotated with @Root in " + controller.getClass().getName());
				}
				if (!Parent.class.isAssignableFrom(field.getType())) {
					throw new FrameworkException("@Root field must be of type Parent or subclass: " + field.getName());
				}
				rootField = field;
			}
		}

		if (rootField == null) {
			throw new FrameworkException("No field annotated with @Root found in " + controller.getClass().getName()
					+ ". Add: @Root private Parent root;");
		}

		rootField.setAccessible(true);
		try {
			rootField.set(controller, root);
		} catch (IllegalAccessException e) {
			throw new FrameworkException("Failed to assign FXML root to @Root field", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> findMatchingConstructor(Class<T> clazz, Object... args)
			throws NoSuchMethodException {
		for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
			if (ctor.getParameterCount() != args.length) {
				continue;
			}
			Class<?>[] types = ctor.getParameterTypes();
			boolean match = true;
			for (int i = 0; i < args.length && match; i++) {
				Class<?> type = types[i].isPrimitive() ? WRAPPERS.get(types[i]) : types[i];
				match = args[i] == null ? !types[i].isPrimitive() : type.isAssignableFrom(args[i].getClass());
			}
			if (match) {
				return (Constructor<T>) ctor;
			}
		}
		throw new NoSuchMethodException(
				"No matching constructor found for " + clazz.getName() + " with " + args.length + " argument(s)");
	}
}