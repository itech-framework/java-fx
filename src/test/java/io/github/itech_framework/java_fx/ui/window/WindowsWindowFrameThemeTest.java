package io.github.itech_framework.java_fx.ui.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@EnabledOnOs(OS.WINDOWS)
class WindowsWindowFrameThemeTest {

    private interface DwmRead extends StdCallLibrary {
        int DwmGetWindowAttribute(Pointer hwnd, int attribute, IntByReference value, int size);
    }

    @BeforeAll
    static void startToolkit() {
        boolean available;
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(ready::countDown);
            available = ready.await(20, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            available = true;
        } catch (Throwable unavailable) {
            available = false;
        }
        Platform.setImplicitExit(false);
        Assumptions.assumeTrue(available, "The JavaFX toolkit is not available in this environment");
    }

    @Test
    void setsAndClearsTheImmersiveDarkAttribute() throws Exception {
        WindowsWindowFrameTheme theme = new WindowsWindowFrameTheme();
        DwmRead dwm = Native.load("dwmapi", DwmRead.class);
        AtomicReference<Stage> holder = new AtomicReference<>();
        AtomicReference<Pointer> handle = new AtomicReference<>();

        run(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane(), 200, 100));
            stage.show();
            holder.set(stage);
            theme.apply(stage, true);
            handle.set(theme.resolveHandle(stage));
        });

        assertNotNull(handle.get());
        assertEquals(1, read(dwm, handle.get()));

        run(() -> theme.apply(holder.get(), false));
        assertEquals(0, read(dwm, handle.get()));

        run(() -> holder.get().close());
    }

    private static int read(DwmRead dwm, Pointer hwnd) {
        IntByReference value = new IntByReference(-1);
        int result = dwm.DwmGetWindowAttribute(hwnd, 20, value, Integer.BYTES);
        assertEquals(0, result);
        return value.getValue();
    }

    private static void run(Runnable action) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                done.countDown();
            }
        });
        done.await(10, TimeUnit.SECONDS);
    }
}