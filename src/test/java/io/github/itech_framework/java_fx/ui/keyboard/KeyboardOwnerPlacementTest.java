package io.github.itech_framework.java_fx.ui.keyboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

class KeyboardOwnerPlacementTest {

    private Stage owner;
    private Stage dialog;
    private Stage nested;

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
        onFx(KeyboardController::install);
    }

    @AfterEach
    void closeWindows() {
        onFx(() -> {
            for (Stage stage : new Stage[] { nested, dialog, owner }) {
                if (stage != null) {
                    stage.close();
                }
            }
        });
        owner = null;
        dialog = null;
        nested = null;
    }

    private static void onFx(Runnable action) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                done.countDown();
            }
        });
        try {
            assertTrue(done.await(20, TimeUnit.SECONDS));
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (failure.get() != null) {
            throw new IllegalStateException(failure.get());
        }
    }

    private static Stage stage(Stage ownerStage, Region content, double x, double y, double width, double height) {
        Stage stage = new Stage();
        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }
        stage.setScene(new Scene(content, width, height));
        stage.setX(x);
        stage.setY(y);
        stage.show();
        return stage;
    }

    private static Popup keyboardPopup() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Popup popup && popup.isShowing() && popup.getContent().size() == 1
                    && popup.getContent().get(0) instanceof VirtualKeyboard) {
                return popup;
            }
        }
        return null;
    }

    private static double centerX(double x, double width) {
        return x + width / 2;
    }

    private static VirtualKeyboard content(Popup popup) {
        return (VirtualKeyboard) popup.getContent().get(0);
    }

    @Test
    void aDialogKeyboardIsSizedAndPlacedAgainstItsOwner() {
        TextField field = new TextField();
        onFx(() -> {
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double ownerX = screen.getMinX() + 20;
            double ownerY = screen.getMinY() + 20;
            double ownerWidth = Math.min(1000, screen.getWidth() - 80);
            double ownerHeight = Math.min(700, screen.getHeight() - 120);
            owner = stage(null, new VBox(), ownerX, ownerY, ownerWidth, ownerHeight);
            dialog = stage(owner, new VBox(field), ownerX + 300, ownerY + 210, 320, 160);
            field.requestFocus();
        });

        Popup popup = keyboardPopup();
        assertNotNull(popup);
        VirtualKeyboard keyboard = content(popup);
        assertTrue(keyboard.getWidth() > dialog.getWidth() + 50,
                "keyboard width " + keyboard.getWidth() + " should not be limited by the dialog " + dialog.getWidth());
        assertEquals(centerX(owner.getX(), owner.getWidth()), centerX(popup.getX(), keyboard.getWidth()), 2);
        assertEquals(owner.getY() + owner.getHeight(), popup.getY() + keyboard.getHeight(), 2);
    }

    @Test
    void aWindowWithoutAnOwnerKeepsUsingItself() {
        TextField field = new TextField();
        onFx(() -> {
            dialog = stage(null, new VBox(field), 350, 250, 420, 300);
            field.requestFocus();
        });

        Popup popup = keyboardPopup();
        assertNotNull(popup);
        VirtualKeyboard keyboard = content(popup);
        assertTrue(keyboard.getWidth() <= dialog.getWidth() + 1,
                "keyboard width " + keyboard.getWidth() + " should fit the window width " + dialog.getWidth());
        assertEquals(centerX(dialog.getX(), dialog.getWidth()), centerX(popup.getX(), keyboard.getWidth()), 2);
    }

    @Test
    void anAnchorIdIsLookedUpInTheOwnersSceneBeforeTheDialogsOwn() {
        TextField field = new TextField();
        Region anchor = new Region();
        anchor.setId("keyboardArea");
        anchor.setPrefSize(600, 80);
        anchor.setMaxSize(600, 80);
        VBox ownerRoot = new VBox(anchor);
        onFx(() -> {
            owner = stage(null, ownerRoot, 50, 40, 1000, 700);
            dialog = stage(owner, new VBox(field), 350, 600, 320, 100);
            KeyboardAnchors.assign(field, "keyboardArea");
            field.requestFocus();
        });

        Popup popup = keyboardPopup();
        assertNotNull(popup);
        VirtualKeyboard keyboard = content(popup);
        Bounds area = anchor.localToScreen(anchor.getLayoutBounds());
        assertEquals(centerX(area.getMinX(), area.getWidth()), centerX(popup.getX(), keyboard.getWidth()), 2);
        assertTrue(popup.getY() >= area.getMaxY(), "keyboard y " + popup.getY() + " should be below " + area.getMaxY());
    }

    @Test
    void nestedDialogsFollowTheOutermostOwner() {
        TextField field = new TextField();
        onFx(() -> {
            owner = stage(null, new VBox(), 50, 40, 1000, 700);
            dialog = stage(owner, new VBox(), 300, 100, 500, 200);
            nested = stage(dialog, new VBox(field), 400, 130, 260, 120);
            field.requestFocus();
        });

        Popup popup = keyboardPopup();
        assertNotNull(popup);
        VirtualKeyboard keyboard = content(popup);
        assertEquals(centerX(owner.getX(), owner.getWidth()), centerX(popup.getX(), keyboard.getWidth()), 2);
        assertTrue(keyboard.getWidth() > nested.getWidth() + 50);
    }

    @Test
    void closingADialogWhileTheKeyboardIsShownDoesNotFailAndHidesTheKeyboard() {
        TextField field = new TextField();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        onFx(() -> {
            owner = stage(null, new VBox(), 50, 40, 1000, 700);
            dialog = stage(owner, new VBox(field), 350, 250, 320, 160);
            field.requestFocus();
        });
        assertNotNull(keyboardPopup());

        onFx(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> failure.set(error));
            dialog.close();
        });

        assertEquals(null, failure.get());
        assertEquals(null, keyboardPopup());
    }
}