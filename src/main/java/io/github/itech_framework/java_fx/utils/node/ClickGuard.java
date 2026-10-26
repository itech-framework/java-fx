package io.github.itech_framework.java_fx.utils.node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.github.itech_framework.java_fx.utils.FxThread;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;

public final class ClickGuard {

    private static final Duration COOLDOWN = Duration.millis(400);

    private ClickGuard() {
    }

    public static void bind(Button button, Runnable action) {
        bind(button, done -> {
            try {
                action.run();
            } finally {
                PauseTransition cooldown = new PauseTransition(COOLDOWN);
                cooldown.setOnFinished(e -> done.run());
                cooldown.play();
            }
        });
    }

    public static void bind(Button button, Consumer<Runnable> action) {
        boolean[] busy = { false };
        button.setOnAction(event -> {
            if (busy[0]) {
                return;
            }
            busy[0] = true;
            button.setDisable(true);

            AtomicBoolean released = new AtomicBoolean();
            Runnable done = () -> {
                if (released.compareAndSet(false, true)) {
                    FxThread.run(() -> {
                        busy[0] = false;
                        button.setDisable(false);
                    });
                }
            };

            try {
                action.accept(done);
            } catch (RuntimeException e) {
                done.run();
                throw e;
            }
        });
    }
}