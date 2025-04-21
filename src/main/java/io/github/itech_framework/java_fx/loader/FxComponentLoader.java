package io.github.itech_framework.java_fx.loader;

import io.github.itech_framework.core.processor.components_processor.ComponentProcessor;
import io.github.itech_framework.java_fx.annotations.components.FxComponent;
import io.github.itech_framework.java_fx.annotations.components.Root;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.stream.IntStream;

public class FxComponentLoader {
    public static <T> T load(Class<T> clazz, Object... constructorArgs) {
        try {
            Constructor<T> constructor = findMatchingConstructor(clazz, constructorArgs);
            constructor.setAccessible(true);
            T instance = constructor.newInstance(constructorArgs);

            FxComponent annotation = clazz.getAnnotation(FxComponent.class);
            if (annotation != null) {
                String fxmlPath = annotation.value().isEmpty()
                        ? "/views/" + clazz.getSimpleName() + ".fxml"
                        : annotation.value();
                FXMLLoader loader = new FXMLLoader(clazz.getResource(fxmlPath));
                loader.setController(instance);
                Parent fxmlRoot = loader.load();

                injectRootAnnotatedField(instance, fxmlRoot);

                ComponentProcessor.injectFields(clazz, instance);
                ComponentProcessor.injectMethods(clazz, instance, ComponentProcessor.DEFAULT_LEVEL);
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create and initialize FXML class: " + clazz.getName(), e);
        }
    }

    private static <T> void injectRootAnnotatedField(T controller, Parent root) {
        Field[] fields = controller.getClass().getDeclaredFields();
        Field rootField = null;

        for (Field field : fields) {
            if (field.isAnnotationPresent(Root.class)) {
                if (rootField != null) {
                    throw new RuntimeException("Multiple fields annotated with @Root in " + controller.getClass().getName());
                }

                if (!Parent.class.isAssignableFrom(field.getType())) {
                    throw new RuntimeException("@Root field must be of type Parent or subclass: " + field.getName());
                }

                rootField = field;
            }
        }

        if (rootField == null) {
            throw new RuntimeException("No field annotated with @Root found in " + controller.getClass().getName());
        }

        rootField.setAccessible(true);
        try {
            rootField.set(controller, root);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to assign FXML root to @Root field", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findMatchingConstructor(Class<T> clazz, Object... args) throws NoSuchMethodException {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == args.length) {
                boolean match = IntStream.range(0, args.length).allMatch(i ->
                        args[i] == null || ctor.getParameterTypes()[i].isAssignableFrom(args[i].getClass())
                );
                if (match) {
                    return (Constructor<T>) ctor;
                }
            }
        }
        throw new NoSuchMethodException("No matching constructor found for " + clazz.getName());
    }
}
