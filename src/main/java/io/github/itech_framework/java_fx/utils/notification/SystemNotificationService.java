package io.github.itech_framework.java_fx.utils.notification;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.stage.Stage;

public class SystemNotificationService {

    public enum Type {
        INFO, WARNING, ERROR, NONE
    }

    private static final Logger logger = LogManager.getLogger(SystemNotificationService.class);
    private static volatile SystemNotificationService instance;

    private volatile TrayIcon trayIcon;
    private volatile Stage stage;
    private volatile Runnable pendingAction;
    private boolean installed;

    public static SystemNotificationService getInstance() {
        SystemNotificationService local = instance;
        if (local == null) {
            synchronized (SystemNotificationService.class) {
                local = instance;
                if (local == null) {
                    local = new SystemNotificationService();
                    instance = local;
                }
            }
        }
        return local;
    }

    private SystemNotificationService() {
    }

    public synchronized void install(Stage primaryStage, String tooltip, InputStream icon) {
        this.stage = primaryStage;
        if (installed) {
            return;
        }
        System.setProperty("java.awt.headless", "false");
        if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
            logger.warn("System tray is not available, system notifications are disabled");
            return;
        }
        installed = true;
        Image image = loadImage(icon);
        EventQueue.invokeLater(() -> addTrayIcon(image, tooltip));
    }

    public void show(String title, String message, Type type) {
        show(title, message, type, null);
    }

    public void show(String title, String message, Type type, Runnable onClick) {
        TrayIcon icon = trayIcon;
        if (icon == null) {
            logger.info("System notification skipped, tray icon is not installed");
            return;
        }
        logger.info("Showing system notification '{}'", title);
        pendingAction = onClick;
        TrayIcon.MessageType kind = switch (type == null ? Type.INFO : type) {
            case INFO -> TrayIcon.MessageType.INFO;
            case WARNING -> TrayIcon.MessageType.WARNING;
            case ERROR -> TrayIcon.MessageType.ERROR;
            case NONE -> TrayIcon.MessageType.NONE;
        };
        EventQueue.invokeLater(() -> icon.displayMessage(title, message, kind));
    }

    public void showIfInactive(String title, String message, Type type, Runnable onClick, Runnable inAppFallback) {
        Runnable decide = () -> {
            if (trayIcon == null || isActive()) {
                if (inAppFallback != null) {
                    inAppFallback.run();
                }
            } else {
                show(title, message, type, onClick);
            }
        };
        if (Platform.isFxApplicationThread()) {
            decide.run();
        } else {
            Platform.runLater(decide);
        }
    }

    public boolean isActive() {
        Stage current = stage;
        return current != null && current.isShowing() && !current.isIconified() && current.isFocused();
    }

    public void shutdown() {
        TrayIcon icon = trayIcon;
        trayIcon = null;
        if (icon != null) {
            SystemTray.getSystemTray().remove(icon);
        }
    }

    private void addTrayIcon(Image image, String tooltip) {
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Open");
        open.addActionListener(event -> Platform.runLater(this::bringToFront));
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener(event -> Platform.runLater(Platform::exit));
        menu.add(open);
        menu.addSeparator();
        menu.add(exit);

        TrayIcon icon = new TrayIcon(image, tooltip, menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(event -> Platform.runLater(this::onActivated));
        try {
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
        } catch (AWTException e) {
            logger.error("Could not add the tray icon", e);
        }
    }

    private void onActivated() {
        bringToFront();
        Runnable action = pendingAction;
        pendingAction = null;
        if (action != null) {
            action.run();
        }
    }

    private void bringToFront() {
        Stage current = stage;
        if (current == null) {
            return;
        }
        if (current.isIconified()) {
            current.setIconified(false);
        }
        if (!current.isShowing()) {
            current.show();
        }
        current.toFront();
        current.requestFocus();
    }

    private static Image loadImage(InputStream stream) {
        if (stream != null) {
            try (InputStream in = stream) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image;
                }
            } catch (IOException e) {
                logger.warn("Could not read the tray icon image", e);
            }
        }
        BufferedImage fallback = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fallback.createGraphics();
        graphics.setColor(new Color(0x0078D7));
        graphics.fillRect(0, 0, 32, 32);
        graphics.dispose();
        return fallback;
    }
}