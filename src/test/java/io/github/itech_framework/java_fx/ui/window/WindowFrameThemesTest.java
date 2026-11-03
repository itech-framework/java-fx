package io.github.itech_framework.java_fx.ui.window;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.github.itech_framework.java_fx.config.FxSettings;

class WindowFrameThemesTest {

    @AfterEach
    void cleanUp() {
        WindowFrameThemes.reset();
        FxSettings.reset();
    }

    @Test
    void darkFrameIsDisabledByDefault() {
        assertFalse(FxSettings.get().windowFrame().darkFrame());
    }

    @Test
    void darkFrameCanBeEnabled() {
        FxSettings.override("fx.window.dark-frame", true);
        assertTrue(FxSettings.get().windowFrame().darkFrame());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void fallsBackToNoOpOutsideWindows() {
        assertFalse(WindowFrameThemes.get().isSupported());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void usesNativeThemeOnWindows() {
        assertTrue(WindowFrameThemes.get().isSupported());
    }
}