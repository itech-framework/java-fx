package io.github.itech_framework.java_fx.ui.keyboard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.annotations.ui.Keyboard;
import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.scene.control.TextInputControl;

public final class KeyboardBinder {
    private static final Logger logger = LogManager.getLogger(KeyboardBinder.class);

    private KeyboardBinder() {}

    public static void validate(Iterable<?> components) {
        List<String> problems = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        for (Object component : components) {
            for (Class<?> type = component.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                if (!seen.add(type)) {
                    continue;
                }
                for (Field field : type.getDeclaredFields()) {
                    Keyboard annotation = field.getAnnotation(Keyboard.class);
                    if (annotation != null) {
                        check(type, field, annotation, problems);
                    }
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new FrameworkException("Invalid @Keyboard usage:\n - " + String.join("\n - ", problems));
        }
    }

    public static void bind(Object controller) {
        for (Class<?> type = controller.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Keyboard annotation = field.getAnnotation(Keyboard.class);
                if (annotation != null) {
                    bindField(controller, type, field, annotation);
                }
            }
        }
    }

    private static void check(Class<?> owner, Field field, Keyboard annotation, List<String> problems) {
        String where = owner.getSimpleName() + "." + field.getName();
        if (!TextInputControl.class.isAssignableFrom(field.getType())) {
            problems.add(where + ": @Keyboard only works on text input fields (TextField, PasswordField, TextArea), found "
                    + field.getType().getSimpleName());
            return;
        }
        int sources = 0;
        if (!annotation.layout().isBlank()) {
            sources++;
        }
        if (annotation.rows().length > 0) {
            sources++;
        }
        if (annotation.supplier() != KeyboardLayoutSupplier.class) {
            sources++;
        }
        if (sources > 1) {
            problems.add(where + ": use only one of layout, rows or supplier");
        }
        if (annotation.rows().length > 0) {
            try {
                KeyboardRows.parse("check", annotation.rows());
            } catch (IllegalArgumentException e) {
                problems.add(where + ": invalid rows, " + e.getMessage());
            }
        }
        if (annotation.supplier() != KeyboardLayoutSupplier.class) {
            try {
                annotation.supplier().getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                problems.add(where + ": supplier " + annotation.supplier().getSimpleName() + " needs a no-argument constructor");
            }
        }
        if (annotation.minWidth() > 0 && annotation.maxWidth() > 0 && annotation.minWidth() > annotation.maxWidth()) {
            problems.add(where + ": minWidth is greater than maxWidth");
        }
    }

    private static void bindField(Object controller, Class<?> owner, Field field, Keyboard annotation) {
        if (!FxSettings.get().keyboard().enabled()) {
            return;
        }
        String where = owner.getSimpleName() + "." + field.getName();
        TextInputControl input;
        try {
            field.setAccessible(true);
            input = (TextInputControl) field.get(controller);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new FrameworkException("Cannot read " + where + " for @Keyboard", e);
        }
        if (input == null) {
            throw new FrameworkException(where + " is annotated with @Keyboard but is null after loading the FXML. "
                    + "Add fx:id=\"" + field.getName() + "\" to the control in the FXML file.");
        }

        KeyboardOptions base = KeyboardOptions.defaults(input);
        String layoutId = resolveLayout(owner, field, annotation, input, where);
        List<String> cycle = annotation.layouts().length > 0 ? List.of(annotation.layouts()) : base.layouts();
        for (String id : cycle) {
            requireRegistered(id, where);
        }

        KeyboardOptions options = new KeyboardOptions(
                annotation.enabled(),
                layoutId != null ? layoutId : base.layout(),
                cycle,
                annotation.anchor().isBlank() ? base.anchor() : annotation.anchor().trim(),
                pick(annotation.keyHeight(), base.keyHeight()),
                pick(annotation.keyGap(), base.keyGap()),
                pick(annotation.maxKeyWidth(), base.maxKeyWidth()),
                pick(annotation.minWidth(), base.minWidth()),
                pick(annotation.maxWidth(), base.maxWidth()));

        input.getProperties().put(KeyboardOptions.PROPERTY, options);
        if (layoutId != null) {
            KeyboardLayouts.assign(input, layoutId);
        }
        if (!options.anchor().isBlank()) {
            KeyboardAnchors.assign(input, options.anchor());
        }
        logger.debug("Keyboard bound to {}", where);
    }

    private static String resolveLayout(Class<?> owner, Field field, Keyboard annotation, TextInputControl input, String where) {
        if (!annotation.layout().isBlank()) {
            String id = annotation.layout().trim();
            requireRegistered(id, where);
            return id;
        }
        if (annotation.rows().length > 0) {
            KeyboardLayout layout = KeyboardRows.parse("field:" + owner.getName() + "#" + field.getName(), annotation.rows());
            KeyboardLayouts.register(layout);
            return layout.id();
        }
        if (annotation.supplier() != KeyboardLayoutSupplier.class) {
            KeyboardLayout layout;
            try {
                Constructor<? extends KeyboardLayoutSupplier> constructor = annotation.supplier().getDeclaredConstructor();
                constructor.setAccessible(true);
                layout = constructor.newInstance().create(input);
            } catch (ReflectiveOperationException e) {
                throw new FrameworkException(where + ": cannot create keyboard supplier " + annotation.supplier().getName(), e);
            }
            if (layout == null) {
                throw new FrameworkException(where + ": keyboard supplier " + annotation.supplier().getSimpleName() + " returned null");
            }
            KeyboardLayouts.register(layout);
            return layout.id();
        }
        return KeyboardConfig.current().layoutFor(input);
    }

    private static void requireRegistered(String id, String where) {
        if (KeyboardLayouts.get(id).isEmpty()) {
            throw new FrameworkException(where + ": keyboard layout '" + id + "' is not registered. Registered layouts: "
                    + KeyboardLayouts.ids());
        }
    }

    private static double pick(double annotationValue, double fallback) {
        return annotationValue >= 0 ? annotationValue : fallback;
    }
}