package io.github.itech_framework.java_fx.utils.concurrent;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javafx.concurrent.Task;

public class TaskScope implements AutoCloseable {
    private final Set<Future<?>> futures = ConcurrentHashMap.newKeySet();
    private final AtomicInteger generation = new AtomicInteger();
    private volatile boolean closed;

    public <T> Task<T> run(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        return run(task, onSuccess, onError, null);
    }

    public <T> Task<T> run(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError,
                           Consumer<Double> onProgress) {
        if (closed) {
            return null;
        }
        int gen = generation.get();
        BooleanSupplier active = () -> !closed && generation.get() == gen;

        Consumer<T> safeSuccess = onSuccess == null ? null : result -> {
            if (active.getAsBoolean()) {
                onSuccess.accept(result);
            }
        };
        Consumer<Exception> safeError = onError == null ? null : error -> {
            if (active.getAsBoolean()) {
                onError.accept(error);
            }
        };
        Consumer<Double> safeProgress = onProgress == null ? null : progress -> {
            if (active.getAsBoolean()) {
                onProgress.accept(progress);
            }
        };

        Task<T> fxTask = BackgroundTaskService.getInstance()
                .executeTask(task, safeSuccess, safeError, safeProgress);
        track(fxTask);
        return fxTask;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Task<?> runProgress(BackgroundTaskService.ProgressTask task) {
        return run(task, task::onSuccess, task::onError, task::onProgress);
    }

    public Future<?> runRunnable(Runnable task) {
        if (closed) {
            return null;
        }
        Future<?> future = BackgroundTaskService.getInstance().executeRunnable(task);
        track(future);
        return future;
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (closed) {
            return null;
        }
        ScheduledFuture<?> future = BackgroundTaskService.getInstance().scheduleTask(task, delay, unit);
        track(future);
        return future;
    }

    public void cancelAll() {
        generation.incrementAndGet();
        for (Future<?> future : futures) {
            future.cancel(true);
            futures.remove(future);
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
        cancelAll();
    }

    private void track(Future<?> future) {
        futures.removeIf(Future::isDone);
        futures.add(future);
        if (closed) {
            future.cancel(true);
            futures.remove(future);
        }
    }
}