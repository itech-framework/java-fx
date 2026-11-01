package io.github.itech_framework.java_fx.utils.notification;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.utils.PropertiesLoader;
import io.github.itech_framework.java_fx.config.FxSettings;
import io.github.itech_framework.java_fx.utils.concurrent.TaskScope;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationCenter {

	private static final Logger logger = LogManager.getLogger(NotificationCenter.class);
	private static volatile NotificationCenter instance;

	private final List<NotificationSource> sources = new CopyOnWriteArrayList<>();
	private final Map<String, Long> fired = new ConcurrentHashMap<>();
	private final TaskScope scope = new TaskScope();
	private final AtomicBoolean started = new AtomicBoolean();
	private Timeline timeline;
	private volatile Consumer<AppNotification> inAppHandler;

	private final Thread shutdownHook = new Thread(this::releaseResources, "fx-notification-shutdown");

	public static NotificationCenter getInstance() {
		NotificationCenter local = instance;
		if (local == null) {
			synchronized (NotificationCenter.class) {
				local = instance;
				if (local == null) {
					local = new NotificationCenter();
					instance = local;
				}
			}
		}
		return local;
	}

	private NotificationCenter() {
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	public void attach(Class<?> primaryClass, Stage stage) {
	    FxSettings.Notification settings = FxSettings.get().notification();
	    if (!settings.enabled() || !settings.tray()) {
	        return;
	    }
	    InputStream icon = null;
	    if (!settings.trayIcon().isBlank()) {
	        icon = primaryClass.getResourceAsStream(settings.trayIcon());
	        if (icon == null) {
	            logger.warn("Tray icon not found on the classpath: {} (using the default icon)", settings.trayIcon());
	        }
	    }
	    SystemNotificationService.getInstance().install(stage, settings.trayTooltip(), icon);
	}

	public void register(NotificationSource source) {
		if (source == null || !FxSettings.get().notification().enabled()) {
			return;
		}
		if (!sourceEnabled(source)) {
			logger.info("Notification source '{}' is disabled by configuration", source.name());
			return;
		}
		if (!sources.contains(source)) {
			sources.add(source);
		}
		ensureStarted();
	}

	public void setInAppHandler(Consumer<AppNotification> handler) {
		this.inAppHandler = handler;
	}

	public void publish(AppNotification notification) {
		if (notification != null && FxSettings.get().notification().enabled()) {
			Platform.runLater(() -> deliver(List.of(notification)));
		}
	}

	public void stop() {
		started.set(false);
		if (timeline != null) {
			timeline.stop();
			timeline = null;
		}
		try {
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		} catch (IllegalStateException ignored) {
		}
		releaseResources();
	}

	public static void stopInstance() {
		NotificationCenter local;
		synchronized (NotificationCenter.class) {
			local = instance;
			instance = null;
		}
		if (local != null) {
			local.stop();
		}
	}

	private boolean sourceEnabled(NotificationSource source) {
		String value = PropertiesLoader.getProperty("fx.notification.source." + source.name() + ".enabled", "true");
		return value == null || !value.trim().equalsIgnoreCase("false");
	}

	private void ensureStarted() {
		if (!started.compareAndSet(false, true)) {
			return;
		}
		long seconds = FxSettings.get().notification().pollSeconds();
		Platform.runLater(() -> {
			if (!started.get()) {
				return;
			}
			timeline = new Timeline(new KeyFrame(Duration.seconds(seconds), event -> poll()));
			timeline.setCycleCount(Animation.INDEFINITE);
			timeline.play();
			poll();
		});
	}

	private void releaseResources() {
		scope.cancelAll();
		SystemNotificationService.shutdownInstance();
	}

	private void poll() {
		LocalDateTime now = LocalDateTime.now();
		long limit = System.currentTimeMillis() - FxSettings.get().notification().keepHours() * 3_600_000L;
		fired.values().removeIf(time -> time < limit);
		for (NotificationSource source : sources) {
			scope.run(() -> source.check(now), this::deliver,
					error -> logger.warn("Notification source '{}' failed: {}", source.name(), error.getMessage()));
		}
	}

	private void deliver(List<AppNotification> notifications) {
		int max = FxSettings.get().notification().maxPerCycle();
		List<AppNotification> fresh = new ArrayList<>();
		for (AppNotification notification : notifications) {
			if (notification.id() == null || !fired.containsKey(notification.id())) {
				fresh.add(notification);
			} else {
				logger.debug("Notification '{}' skipped: already fired", notification.id());
			}
		}
		logger.debug("Deliver: {} received, {} new, maxPerCycle={}", notifications.size(), fresh.size(), max);
		int shown = Math.min(fresh.size(), max);
		for (int i = 0; i < shown; i++) {
			AppNotification notification = fresh.get(i);
			if (show(notification) && notification.id() != null) {
				fired.put(notification.id(), System.currentTimeMillis());
			}
		}
		if (fresh.size() > shown) {
			show(new AppNotification(null, (fresh.size() - shown) + " more notifications", "Open the app to see them",
					SystemNotificationService.Type.INFO, null));
		}
	}

	private boolean show(AppNotification notification) {
		Consumer<AppNotification> handler = inAppHandler;
		SystemNotificationService service = SystemNotificationService.getInstance();
		logger.debug("Show '{}': inAppHandler={}, windowActive={}", notification.title(), handler != null,
				service.isActive());
		if (handler == null) {
			return service.show(notification.title(), notification.message(), notification.type(),
					notification.onClick());
		}
		service.showIfInactive(notification.title(), notification.message(), notification.type(),
				notification.onClick(), () -> {
					logger.debug("Notification '{}' sent to in-app handler", notification.title());
					handler.accept(notification);
				});
		return true;
	}
}