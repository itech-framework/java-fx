package io.github.itech_framework.java_fx.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.core.utils.PropertiesLoader;
import io.github.itech_framework.java_fx.router.config.DisposePolicy;
import io.github.itech_framework.java_fx.ui.toast.ToastPosition;
import javafx.scene.paint.Color;

public final class FxSettings {

    public enum PreloaderTheme { AUTO, LIGHT, DARK }

    public record App(String name, boolean exitOnStop, boolean controllersRequired, boolean smoothFont) {}

    public record Theme(boolean enabled, String stylesheet, String darkModeKey, String darkStyleClass,
                        Color lightFill, Color darkFill) {}

    public record Preloader(boolean enabled, String stylesheet, PreloaderTheme theme, boolean decorated,
                            double width, double height, double barWidth, double barHeight, double titleFontSize,
                            String startingMessage, String initializingMessage, String injectingMessage,
                            String preparingMessage, String registeringMessage, String finalizingMessage,
                            String failedMessage, long delay) {}

    public record Router(boolean validateOnInit, boolean runOnFxThread, DisposePolicy disposePolicy,
                         String contentContainerId, int maxRedirects) {}

    public record Components(String fxmlBase) {}

    public record Background(int threads, int scheduledThreads, boolean daemon, long shutdownWaitMillis) {}

    public record Toast(ToastPosition position, long durationMillis, double offsetX, double offsetY,
                        double maxWidth, double maxHeight) {}
    
    public record Scroll(boolean smooth, double speed, double responseMillis, boolean contentCaching) {}

    public record Notification(boolean enabled, boolean tray, String trayIcon, String trayTooltip,
            long pollSeconds, int maxPerCycle, long keepHours) {}
    
    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();
    private static volatile FxSettings current;

    private final App app;
    private final Theme theme;
    private final Preloader preloader;
    private final Router router;
    private final Components components;
    private final Background background;
    private final Toast toast;
    
    private final Scroll scroll;

    private final Notification notification;

    private FxSettings(App app, Theme theme, Preloader preloader, Router router, Components components,
                       Background background, Toast toast, Scroll scroll, Notification notification) {
        this.app = app;
        this.theme = theme;
        this.preloader = preloader;
        this.router = router;
        this.components = components;
        this.background = background;
        this.toast = toast;
        this.scroll = scroll;
        this.notification = notification;
    }

    public Notification notification() { return notification; }

    public Scroll scroll() { return scroll; }

    public App app() { return app; }
    public Theme theme() { return theme; }
    public Preloader preloader() { return preloader; }
    public Router router() { return router; }
    public Components components() { return components; }
    public Background background() { return background; }
    public Toast toast() { return toast; }

    public static FxSettings get() {
        FxSettings snapshot = current;
        if (snapshot == null) {
            synchronized (FxSettings.class) {
                snapshot = current;
                if (snapshot == null) {
                    snapshot = load();
                    current = snapshot;
                }
            }
        }
        return snapshot;
    }

    public static synchronized FxSettings reload() {
        current = load();
        return current;
    }

    public static synchronized void override(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Setting key must not be blank");
        }
        OVERRIDES.put(key, String.valueOf(value));
        current = null;
    }

    public static synchronized void reset() {
        OVERRIDES.clear();
        current = null;
    }

    private static FxSettings load() {
        Reader in = new Reader();

        App app = new App(
                in.string("app.name", "Application"),
                in.bool("fx.app.exit-on-stop", true),
                in.bool("fx.controllers.required", true),
                in.bool("fx.app.font.smooth", true));

        Theme theme = new Theme(
                in.bool("fx.theme.enabled", true),
                in.string("fx.theme.stylesheet", "/static/css/style.css"),
                in.string("fx.theme.dark-mode-key", "isDarkMode"),
                in.string("fx.theme.dark-style-class", "dark-mode"),
                in.color("fx.theme.light-fill", "#FFFFFF"),
                in.color("fx.theme.dark-fill", "#000000"));

        Preloader preloader = new Preloader(
                in.bool("fx.preloader.enabled", true),
                in.string("fx.preloader.stylesheet", ""),
                in.enumeration("fx.preloader.theme", PreloaderTheme.AUTO, PreloaderTheme.class),
                in.bool("fx.preloader.decorated", false),
                in.number("fx.preloader.width", 400, 100, 4000),
                in.number("fx.preloader.height", 200, 100, 4000),
                in.number("fx.preloader.bar-width", 300, 50, 4000),
                in.number("fx.preloader.bar-height", 15, 2, 200),
                in.number("fx.preloader.title-font-size", 18, 6, 96),
                in.string("fx.preloader.message.starting", "Starting..."),
                in.string("fx.preloader.message.initializing", "Initializing application..."),
                in.string("fx.preloader.message.injecting", "Injecting data..."),
                in.string("fx.preloader.message.preparing", "Preparing components..."),
                in.string("fx.preloader.message.registering", "Registering services..."),
                in.string("fx.preloader.message.finalizing", "Finalizing..."),
                in.string("fx.preloader.message.failed", "Startup failed"),
                in.number("fx.preloader.delay", 0, 0, 100000));

        Router router = new Router(
                in.bool("fx.router.validate-on-init", true),
                in.bool("fx.router.run-on-fx-thread", true),
                in.enumeration("fx.router.dispose-policy", DisposePolicy.ON_LEAVE, DisposePolicy.class),
                in.string("fx.router.content-container-id", "contentContainer"),
                (int) in.number("fx.router.max-redirects", 10, 1, 100));

        Components components = new Components(in.directory("fx.components.fxml-base", "/views/"));

        Background background = new Background(
                (int) in.number("fx.background.threads", 4, 1, 64),
                (int) in.number("fx.background.scheduled-threads", 2, 1, 64),
                in.bool("fx.background.daemon", true),
                (long) in.number("fx.background.shutdown-wait-ms", 1000, 0, 60000));

        Toast toast = new Toast(
                in.enumeration("fx.toast.position", ToastPosition.BOTTOM_RIGHT, ToastPosition.class),
                (long) in.number("fx.toast.duration-ms", 3000, 100, 600000),
                in.number("fx.toast.offset-x", 20, 0, 2000),
                in.number("fx.toast.offset-y", 20, 0, 2000),
                in.number("fx.toast.max-width", 350, 50, 4000),
                in.number("fx.toast.max-height", 200, 30, 4000));

        Scroll scroll = new Scroll(
                in.bool("fx.scroll.smooth", true),
                in.number("fx.scroll.speed", 2.0, 0.1, 20),
                in.number("fx.scroll.response-ms", 80, 10, 1000),
                in.bool("fx.scroll.content-caching", true));

        Notification notification = new Notification(
                in.bool("fx.notification.enabled", true),
                in.bool("fx.notification.tray", true),
                in.string("fx.notification.tray-icon", ""),
                in.string("fx.notification.tray-tooltip", app.name()),
                (long) in.number("fx.notification.poll-seconds", 60, 5, 3600),
                (int) in.number("fx.notification.max-per-cycle", 3, 1, 20),
                (long) in.number("fx.notification.keep-hours", 48, 1, 720));

        in.failOnErrors();
        return new FxSettings(app, theme, preloader, router, components, background, toast, scroll, notification);
    }

    private static final class Reader {
        private final List<String> errors = new ArrayList<>();

        String raw(String key, String fallback) {
            String override = OVERRIDES.get(key);
            if (override != null) {
                return override.trim();
            }
            String value = PropertiesLoader.getProperty(key, fallback);
            return value == null ? fallback : value.trim();
        }

        String string(String key, String fallback) {
            return raw(key, fallback);
        }

        boolean bool(String key, boolean fallback) {
            String value = raw(key, String.valueOf(fallback));
            if (value.equalsIgnoreCase("true")) return true;
            if (value.equalsIgnoreCase("false")) return false;
            errors.add(key + "=" + value + " (expected true or false)");
            return fallback;
        }

        double number(String key, double fallback, double min, double max) {
            String value = raw(key, String.valueOf(fallback));
            try {
                double parsed = Double.parseDouble(value);
                if (parsed < min || parsed > max) {
                    errors.add(key + "=" + value + " (expected a number between " + trim(min) + " and " + trim(max) + ")");
                    return fallback;
                }
                return parsed;
            } catch (NumberFormatException e) {
                errors.add(key + "=" + value + " (expected a number)");
                return fallback;
            }
        }
        
        long number(String key, long fallback, long min, long max) {
            String value = raw(key, String.valueOf(fallback));
            try {
                long parsed = Long.parseLong(value);
                if (parsed < min || parsed > max) {
                    errors.add(key + "=" + value + " (expected a number between " + trim(min) + " and " + trim(max) + ")");
                    return fallback;
                }
                return parsed;
            } catch (NumberFormatException e) {
                errors.add(key + "=" + value + " (expected a number)");
                return fallback;
            }
        }

        <E extends Enum<E>> E enumeration(String key, E fallback, Class<E> type) {
            String value = raw(key, fallback.name());
            String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_');
            for (E constant : type.getEnumConstants()) {
                if (constant.name().equals(normalized)) {
                    return constant;
                }
            }
            errors.add(key + "=" + value + " (expected one of " + List.of(type.getEnumConstants()) + ")");
            return fallback;
        }

        Color color(String key, String fallback) {
            String value = raw(key, fallback);
            try {
                return Color.web(value);
            } catch (IllegalArgumentException | NullPointerException e) {
                errors.add(key + "=" + value + " (expected a color such as #FFFFFF or white)");
                return Color.web(fallback);
            }
        }

        String directory(String key, String fallback) {
            String value = raw(key, fallback);
            if (value.isEmpty()) {
                return fallback;
            }
            return value.endsWith("/") ? value : value + "/";
        }

        void failOnErrors() {
            if (!errors.isEmpty()) {
                throw new FrameworkException("Invalid JavaFX module settings:\n - " + String.join("\n - ", errors));
            }
        }

        private static String trim(double value) {
            return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
    }
}