package org.itech.framework.fx.java_fx.utils.concurrent;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class BackgroundTaskService {
    private static final BackgroundTaskService INSTANCE = new BackgroundTaskService();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    public static BackgroundTaskService getInstance() {
        return INSTANCE;
    }

    private BackgroundTaskService() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public <T> void executeTask(Callable<T> task,
                                Consumer<T> onSuccess,
                                Consumer<Exception> onError) {
        executeTask(task, onSuccess, onError, null);
    }

    public <T> void executeTask(Callable<T> task,
                                Consumer<T> onSuccess,
                                Consumer<Exception> onError,
                                Consumer<Double> onProgress) {
        Service<T> service = new Service<>() {
            @Override
            protected Task<T> createTask() {
                return new Task<>() {
                    @Override
                    protected T call() throws Exception {
                        return task.call();
                    }

                    @Override
                    protected void updateProgress(double workDone, double max) {
                        Platform.runLater(() -> {
                            if (onProgress != null) {
                                onProgress.accept(workDone / max);
                            }
                        });
                        super.updateProgress(workDone, max);
                    }
                };
            }
        };

        service.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(service.getValue());
            }
        });

        service.setOnFailed(e -> {
            Throwable ex = service.getException();
            if (onError != null && ex != null) {
                onError.accept(ex instanceof Exception ?
                        (Exception) ex : new Exception(ex));
            }
        });

        // Proper way to start JavaFX Service
        service.start();
    }

    // Fire-and-forget tasks
    public void executeRunnable(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                handleUncaughtException(e);
            }
        });
    }

    // Scheduled tasks
    public ScheduledFuture<?> scheduleTask(Runnable task,
                                           long delay,
                                           TimeUnit unit) {
        return scheduledExecutor.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                handleUncaughtException(e);
            }
        }, delay, unit);
    }

    // Progress-aware tasks
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void executeProgressTask(ProgressTask task) {
        executeTask(task, task::onSuccess, task::onError, task::onProgress);
    }

    private void handleUncaughtException(Throwable throwable) {
        Platform.runLater(() -> {
            System.err.println("Uncaught background error: " + throwable.getMessage());
            throwable.printStackTrace();
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Background tasks did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Progress-aware task interface
    public interface ProgressTask<T> extends Callable<T> {
        void onSuccess(T result);
        void onError(Exception e);
        void onProgress(double progress);
    }
}