package io.github.itech_framework.java_fx.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javafx.application.Platform;

public final class FxThread {
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private FxThread() {
    }

    public static void run(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static <T> T call(Supplier<T> supplier) {
        return call(supplier, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public static <T> T call(Supplier<T> supplier, long timeout, TimeUnit unit) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            if (future.isDone()) {
                return;
            }
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(false);
            throw new IllegalStateException("The UI thread did not run the action within " + timeout + " " + unit
                    + ". The UI thread may be blocked, for example by code that waits for this thread.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        }
    }
}