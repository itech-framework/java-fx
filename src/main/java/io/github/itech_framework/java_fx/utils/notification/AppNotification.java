package io.github.itech_framework.java_fx.utils.notification;

public record AppNotification(String id, String title, String message, SystemNotificationService.Type type,
        Runnable onClick) {
}