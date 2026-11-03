package io.github.itech_framework.java_fx.ui.window;

import java.lang.reflect.Method;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import javafx.stage.Stage;
import javafx.stage.Window;

final class WindowsWindowFrameTheme implements WindowFrameTheme {
    private static final Logger logger = LogManager.getLogger(WindowsWindowFrameTheme.class);
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY = 19;
    private static final int SWP_REFRESH_FLAGS = 0x0001 | 0x0002 | 0x0004 | 0x0010 | 0x0020;

    private interface Dwmapi extends StdCallLibrary {
        int DwmSetWindowAttribute(Pointer hwnd, int attribute, IntByReference value, int size);
    }

    private interface User32 extends StdCallLibrary {
        Pointer FindWindowW(WString className, WString windowName);

        boolean IsWindow(Pointer hwnd);

        boolean SetWindowPos(Pointer hwnd, Pointer insertAfter, int x, int y, int cx, int cy, int flags);
    }

    private final Dwmapi dwmapi;
    private final User32 user32;

    WindowsWindowFrameTheme() {
        this.dwmapi = Native.load("dwmapi", Dwmapi.class);
        this.user32 = Native.load("user32", User32.class);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void apply(Stage stage, boolean dark) {
        Pointer hwnd = resolveHandle(stage);
        if (hwnd == null) {
            logger.debug("Native window handle was not found for stage '{}'", stage.getTitle());
            return;
        }
        IntByReference value = new IntByReference(dark ? 1 : 0);
        int result = dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, value, Integer.BYTES);
        if (result != 0) {
            result = dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY, value, Integer.BYTES);
        }
        if (result != 0) {
            logger.debug("DwmSetWindowAttribute failed with code {}", result);
            return;
        }
        user32.SetWindowPos(hwnd, null, 0, 0, 0, 0, SWP_REFRESH_FLAGS);
    }

    Pointer resolveHandle(Stage stage) {
        long raw = reflectedHandle(stage);
        if (raw != 0) {
            Pointer candidate = new Pointer(raw);
            if (user32.IsWindow(candidate)) {
                return candidate;
            }
        }
        return handleByTitle(stage);
    }

    private long reflectedHandle(Stage stage) {
        try {
            Class<?> helper = Class.forName("com.sun.javafx.stage.WindowHelper");
            Method getPeer = helper.getMethod("getPeer", Window.class);
            Object peer = getPeer.invoke(null, stage);
            if (peer == null) {
                return 0;
            }
            Class<?> tkStage = Class.forName("com.sun.javafx.tk.TKStage");
            Object value = tkStage.getMethod("getRawHandle").invoke(peer);
            return value instanceof Long handle ? handle : 0;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    private Pointer handleByTitle(Stage stage) {
        String original = stage.getTitle();
        String marker = "itech-frame-" + UUID.randomUUID();
        stage.setTitle(marker);
        try {
            return user32.FindWindowW(null, new WString(marker));
        } finally {
            stage.setTitle(original);
        }
    }
}