package io.github.itech_framework.java_fx.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class EventBus {
    private static final Map<String, Map<String, Consumer<Object>>> listeners = new HashMap<>();

    /**
     * Subscribe a listener with a unique ID
     */
    public static String subscribe(String eventType, Consumer<Object> listener) {
        return subscribe(eventType, UUID.randomUUID().toString(), listener);
    }

    /**
     * Subscribe a listener with a specific ID
     */
    public static String subscribe(String eventType, String listenerId, Consumer<Object> listener) {
        unsubscribe(eventType, listenerId);
    	listeners.computeIfAbsent(eventType, k -> new HashMap<>()).put(listenerId, listener);
        return listenerId;
    }

    /**
     * Unsubscribe using ID
     */
    public static boolean unsubscribe(String eventType, String listenerId) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            return eventListeners.remove(listenerId) != null;
        }
        return false;
    }

    /**
     * Unsubscribe a specific listener from all event types
     */
    public static void unsubscribeFromAll(Consumer<Object> listener) {
        for (Map<String, Consumer<Object>> eventListeners : listeners.values()) {
            eventListeners.values().removeIf(l -> l == listener);
        }
    }

    /**
     * Unsubscribe all listeners from a specific event type
     */
    public static void unsubscribeAll(String eventType) {
        listeners.remove(eventType);
    }

    /**
     * Publish an event to all subscribed listeners
     */
    public static void publish(String eventType, Object data) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            // Create a copy of values to avoid ConcurrentModificationException
            List<Consumer<Object>> listenersCopy = new ArrayList<>(eventListeners.values());
            listenersCopy.forEach(listener -> {
                try {
                    listener.accept(data);
                } catch (Exception e) {
                    // Log the error but continue with other listeners
                    System.err.println("Error in event listener for event: " + eventType);
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Publish an event asynchronously
     */
    public static void publishAsync(String eventType, Object data) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            // Create a copy of values to avoid ConcurrentModificationException
            List<Consumer<Object>> listenersCopy = new ArrayList<>(eventListeners.values());
            listenersCopy.forEach(listener -> {
                // Execute each listener in a separate thread
                new Thread(() -> {
                    try {
                        listener.accept(data);
                    } catch (Exception e) {
                        System.err.println("Error in async event listener for event: " + eventType);
                        e.printStackTrace();
                    }
                }).start();
            });
        }
    }

    /**
     * Publish an event asynchronously using ExecutorService
     */
    public static void publishAsync(String eventType, Object data, java.util.concurrent.ExecutorService executor) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            // Create a copy of values to avoid ConcurrentModificationException
            List<Consumer<Object>> listenersCopy = new ArrayList<>(eventListeners.values());
            listenersCopy.forEach(listener -> {
                executor.submit(() -> {
                    try {
                        listener.accept(data);
                    } catch (Exception e) {
                        System.err.println("Error in async event listener for event: " + eventType);
                        e.printStackTrace();
                    }
                });
            });
        }
    }

    /**
     * Get the number of listeners subscribed to a specific event type
     */
    public static int getListenerCount(String eventType) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    /**
     * Check if there are any listeners subscribed to a specific event type
     */
    public static boolean hasListeners(String eventType) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null && !eventListeners.isEmpty();
    }

    /**
     * Get all listener IDs for a specific event type
     */
    public static Set<String> getListenerIds(String eventType) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? new HashSet<>(eventListeners.keySet()) : Collections.emptySet();
    }

    /**
     * Check if a specific listener ID exists for an event type
     */
    public static boolean hasListener(String eventType, String listenerId) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        return eventListeners != null && eventListeners.containsKey(listenerId);
    }

    /**
     * Get the listener for a specific ID
     */
    public static Optional<Consumer<Object>> getListener(String eventType, String listenerId) {
        Map<String, Consumer<Object>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            return Optional.ofNullable(eventListeners.get(listenerId));
        }
        return Optional.empty();
    }

    /**
     * Clear all event listeners from all event types
     */
    public static void clearAll() {
        listeners.clear();
    }

    /**
     * Get all event types that have listeners
     */
    public static Set<String> getAllEventTypes() {
        return new HashSet<>(listeners.keySet());
    }
}