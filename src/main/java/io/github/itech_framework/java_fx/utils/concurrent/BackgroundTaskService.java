package io.github.itech_framework.java_fx.utils.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.concurrent.Task;

public class BackgroundTaskService {
    private static final Logger logger = LogManager.getLogger(BackgroundTaskService.class);
    private static volatile BackgroundTaskService instance;

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final long shutdownWaitMillis;

    public static BackgroundTaskService getInstance() {
        BackgroundTaskService local = instance;
        if (local == null) {
            synchronized (BackgroundTaskService.class) {
                local = instance;
                if (local == null) {
                    local = new BackgroundTaskService();
                    instance = local;
                }
            }
        }
        return local;
    }

    private BackgroundTaskService() {
        FxSettings.Background settings = FxSettings.get().background();
        this.executor = Executors.newFixedThreadPool(settings.threads(),
                threadFactory("fx-background", settings.daemon()));
        this.scheduledExecutor = Executors.newScheduledThreadPool(settings.scheduledThreads(),
                threadFactory("fx-scheduled", settings.daemon()));
        this.shutdownWaitMillis = settings.shutdownWaitMillis();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "fx-background-shutdown"));
    }

    public <T> void executeTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        executeTask(task, onSuccess, onError, null);
    }

    public <T> void executeTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError,
                                Consumer<Double> onProgress) {
        Task<T> fxTask = new Task<>() {
            @Override
            protected T call() throws Exception {
                return task.call();
            }
        };

        fxTask.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(fxTask.getValue());
            }
        });
        fxTask.setOnFailed(e -> {
            Throwable error = fxTask.getException();
            if (onError != null && error != null) {
                onError.accept(error instanceof Exception ex ? ex : new Exception(error));
            } else {
                logger.error("Background task failed and no error handler was given", error);
            }
        });
        if (onProgress != null) {
            fxTask.progressProperty().addListener((obs, oldValue, newValue) -> {
                double progress = newValue.doubleValue();
                if (progress >= 0) {
                    onProgress.accept(progress);
                }
            });
        }
        executor.execute(fxTask);
    }

    public void executeRunnable(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Uncaught background error", e);
            }
        });
    }

    public ScheduledFuture<?> scheduleTask(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Uncaught scheduled task error", e);
            }
        }, delay, unit);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void executeProgressTask(ProgressTask task) {
        executeTask(task, task::onSuccess, task::onError, task::onProgress);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        try {
            if (!executor.awaitTermination(shutdownWaitMillis, TimeUnit.MILLISECONDS)) {
                logger.warn("Background tasks did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory threadFactory(String prefix, boolean daemon) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(daemon);
            return thread;
        };
    }

    public interface ProgressTask<T> extends Callable<T> {
        void onSuccess(T result);

        void onError(Exception e);

        void onProgress(double progress);
    }
}