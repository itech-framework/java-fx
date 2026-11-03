package io.github.itech_framework.java_fx.ui.window;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WindowFrameThemes {
    private static final Logger logger = LogManager.getLogger(WindowFrameThemes.class);
    private static volatile WindowFrameTheme current;

    private WindowFrameThemes() {
    }

    public static WindowFrameTheme get() {
        WindowFrameTheme theme = current;
        if (theme == null) {
            synchronized (WindowFrameThemes.class) {
                theme = current;
                if (theme == null) {
                    theme = create();
                    current = theme;
                }
            }
        }
        return theme;
    }

    private static WindowFrameTheme create() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            try {
                return new WindowsWindowFrameTheme();
            } catch (RuntimeException | LinkageError e) {
                logger.warn("Native window frame theme is not available on this system", e);
            }
        }
        return new NoOpWindowFrameTheme();
    }
    
    static void use(WindowFrameTheme theme) {
        current = theme;
    }

    static void reset() {
        current = null;
    }
}