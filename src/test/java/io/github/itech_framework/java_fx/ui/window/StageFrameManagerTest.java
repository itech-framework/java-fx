package io.github.itech_framework.java_fx.ui.window;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

class StageFrameManagerTest {
    private static final List<Boolean> applied = new CopyOnWriteArrayList<>();
    private static final AtomicReference<Boolean> darkMode = new AtomicReference<>();
    private Stage stage;

    @BeforeAll
    static void install() {
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

        FxSettings.override("fx.window.dark-frame", true);
        WindowFrameThemes.use(new WindowFrameTheme() {
            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public void apply(Stage target, boolean dark) {
                applied.add(dark);
            }
        });
        StageFrameManager.useDarkModeSource(darkMode::get);
        onFx(StageFrameManager::install);
    }

    @AfterAll
    static void tearDown() {
        onFx(StageFrameManager::resetForTests);
        FxSettings.reset();
        WindowFrameThemes.reset();
    }

    @BeforeEach
    void prepare() {
        applied.clear();
        darkMode.set(true);
    }

    @AfterEach
    void closeStage() {
        onFx(() -> {
            if (stage != null) {
                stage.close();
            }
        });
        stage = null;
    }

    @Test
    void appliesDarkFrameWhenStageIsShown() throws Exception {
        showStage();
        assertTrue(await(() -> applied.contains(true)));
    }

    @Test
    void refreshAppliesTheNewValue() throws Exception {
        showStage();
        assertTrue(await(() -> !applied.isEmpty()));
        applied.clear();
        darkMode.set(false);
        StageFrameManager.refresh();
        assertTrue(await(() -> applied.contains(false)));
    }

    @Test
    void skipsWhenDarkModeIsNotStored() throws Exception {
        darkMode.set(null);
        showStage();
        Thread.sleep(300);
        assertTrue(applied.isEmpty());
    }

    private void showStage() {
        onFx(() -> {
            stage = new Stage();
            stage.setScene(new Scene(new StackPane(), 200, 100));
            stage.show();
        });
    }

    private static boolean await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    private static void onFx(Runnable action) {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                done.countDown();
            }
        });
        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}