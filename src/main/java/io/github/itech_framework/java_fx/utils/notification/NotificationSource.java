package io.github.itech_framework.java_fx.utils.notification;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationSource {

    String name();

    List<AppNotification> check(LocalDateTime now) throws Exception;
}