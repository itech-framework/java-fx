package io.github.itech_framework.java_fx.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.ITechJavaFxApplication;
import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.collections.ObservableList;

public final class FxResources {

    private FxResources() {
    }

    public static URL find(Class<?> context, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        List<Class<?>> candidates = new ArrayList<>();
        if (context != null) candidates.add(context);
        Class<?> main = ITechJavaFxApplication.getMainClass();
        if (main != null) candidates.add(main);
        candidates.add(FxResources.class);

        for (Class<?> candidate : candidates) {
            URL url = candidate.getResource(path);
            if (url != null) {
                return url;
            }
        }
        String classpathPath = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? null : loader.getResource(classpathPath);
    }

    public static URL require(Class<?> context, String path, String purpose) {
        URL url = find(context, path);
        if (url == null) {
            throw new FrameworkException("Resource not found for " + purpose + ": '" + path + "'. "
                    + "Paths are resolved from the classpath root, for example /views/home.fxml "
                    + "(src/main/resources/views/home.fxml).");
        }
        return url;
    }

    public static String themeStylesheet() {
        return require(null, FxSettings.get().theme().stylesheet(), "theme stylesheet (fx.theme.stylesheet)")
                .toExternalForm();
    }

    public static void applyTheme(ObservableList<String> stylesheets) {
        if (!FxSettings.get().theme().enabled()) {
            return;
        }
        String css = themeStylesheet();
        if (!stylesheets.contains(css)) {
            stylesheets.add(css);
        }
    }
}