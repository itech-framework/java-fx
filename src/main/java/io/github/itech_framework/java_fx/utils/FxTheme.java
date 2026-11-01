package io.github.itech_framework.java_fx.utils;

import io.github.itech_framework.core.utils.DataStorageUtil;
import io.github.itech_framework.java_fx.config.FxSettings;

public final class FxTheme {

    private FxTheme() {
    }

    public static Boolean storedDarkMode() {
        try {
            Object stored = DataStorageUtil.load(FxSettings.get().theme().darkModeKey());
            if (stored == null) {
                return null;
            }
            return Boolean.parseBoolean(String.valueOf(stored).replace("\"", "").trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}