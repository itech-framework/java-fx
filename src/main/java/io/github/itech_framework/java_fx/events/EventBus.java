package io.github.itech_framework.java_fx.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
	private static final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

	public static void subscribe(String eventType, Consumer<Object> listener) {
		listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
	}

	public static void publish(String eventType, Object data) {
		List<Consumer<Object>> eventListeners = listeners.get(eventType);
		if (eventListeners != null) {
			eventListeners.forEach(listener -> listener.accept(data));
		}
	}
}
