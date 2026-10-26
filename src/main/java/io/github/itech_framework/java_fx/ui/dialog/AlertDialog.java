package io.github.itech_framework.java_fx.ui.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import io.github.itech_framework.java_fx.utils.FxThread;
import io.github.itech_framework.java_fx.utils.SVGUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AlertDialog extends AbstractFxDialog {

    private static final AtomicReference<AlertDialog> CURRENT = new AtomicReference<>();

    private final VBox content = new VBox(15);
    private final HBox buttonContainer = new HBox(10);
    private volatile Runnable onClose;

    private AlertDialog(Level level, String title, String message, Stage owner, Runnable onClose) {
    	super(owner, 0);
    	this.onClose = onClose;

    	content.setPadding(new Insets(20));
    	content.getStyleClass().addAll("alert-container", "alert-" + level.name().toLowerCase(Locale.ROOT));
        this.onClose = onClose;

        content.setPadding(new Insets(20));
        content.getStyleClass().add("alert-container");

        Node icon = SVGUtil.getIcon(level.svgPath, Color.web(level.color), 24, 24);
        icon.getStyleClass().add("alert-icon");

        Label titleLabel = new Label(title != null ? title : level.title);
        titleLabel.getStyleClass().add("alert-title");

        HBox header = new HBox(10, icon, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);

        buttonContainer.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(header, messageLabel, buttonContainer);
        rootPane.getChildren().add(content);
    }

    public static AlertDialogBuilder builder() {
        return new AlertDialogBuilder();
    }

    @Override
    protected void beforeShow() {
        CURRENT.set(this);
    }

    @Override
    protected void afterClosed() {
        CURRENT.compareAndSet(this, null);
        Runnable callback = onClose;
        if (callback != null) {
            callback.run();
        }
    }

    public void show() {
        showAndWait();
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public static AlertDialog getCurrentAlertDialog() {
        return CURRENT.get();
    }

    public static void setCurrentAlertDialog(AlertDialog currentAlertDialog) {
        CURRENT.set(currentAlertDialog);
    }

    public static class AlertDialogBuilder {
        private Level level = Level.INFO;
        private String title;
        private String message;
        private final List<Button> buttons = new ArrayList<>();
        private final List<String> customFonts = new ArrayList<>();
        private final List<String> stylesheets = new ArrayList<>();
        private Stage owner;
        private AlertDialog dialog;
        private Double minWidth;
        private Double minHeight;
        private Double maxWidth;
        private Double maxHeight;
        private Runnable onClose;

        public AlertDialogBuilder level(Level level) {
            this.level = level;
            return this;
        }

        public AlertDialogBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AlertDialogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AlertDialogBuilder addButton(String text, Runnable action) {
            buttons.add(actionButton(text, "alert-default-button", action));
            return this;
        }

        public AlertDialogBuilder addButton(String text, String styleClass, Runnable action) {
            buttons.add(actionButton(text, styleClass, action));
            return this;
        }

        public AlertDialogBuilder addOwner(Stage owner) {
            this.owner = owner;
            return this;
        }

        public AlertDialogBuilder customFonts(String... fontFamilies) {
            if (fontFamilies != null) {
                for (String font : fontFamilies) {
                    if (!customFonts.contains(font)) {
                        customFonts.add(font);
                    }
                }
            }
            return this;
        }

        public AlertDialogBuilder stylesheets(String... stylesheets) {
            if (stylesheets != null) {
                for (String ss : stylesheets) {
                    if (!this.stylesheets.contains(ss)) {
                        this.stylesheets.add(ss);
                    }
                }
            }
            return this;
        }

        public AlertDialogBuilder minWidth(double minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public AlertDialogBuilder minHeight(double minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        public AlertDialogBuilder maxWidth(double maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public AlertDialogBuilder maxHeight(double maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public AlertDialogBuilder onClose(Runnable onClose) {
            this.onClose = onClose;
            return this;
        }

        public AlertDialog build() {
            AlertDialog created = FxThread.call(() -> {
                AlertDialog previous = CURRENT.get();
                if (previous != null) {
                    previous.closeImmediately();
                }

                AlertDialog d = new AlertDialog(level, title, message, owner, onClose);
                this.dialog = d;

                if (minWidth != null) {
                    d.rootPane.setMinWidth(minWidth);
                    d.content.setMinWidth(minWidth - 30);
                }
                if (minHeight != null) {
                    d.rootPane.setMinHeight(minHeight);
                    d.content.setMinHeight(minHeight - 30);
                }
                if (maxWidth != null) {
                    d.rootPane.setMaxWidth(maxWidth);
                    d.content.setMaxWidth(maxWidth - 30);
                }
                if (maxHeight != null) {
                    d.rootPane.setMaxHeight(maxHeight);
                    d.content.setMaxHeight(maxHeight - 30);
                }

                if (buttons.isEmpty()) {
                    Button ok = actionButton("OK", "alert-default-button", null);
                    ok.setDefaultButton(true);
                    d.buttonContainer.getChildren().add(ok);
                } else {
                    d.buttonContainer.getChildren().addAll(buttons);
                }

                d.applyStylesheets(stylesheets);
                d.applyFonts(customFonts);
                return d;
            });
            return created;
        }

        private Button actionButton(String text, String styleClass, Runnable action) {
            Button btn = new Button(text);
            btn.getStyleClass().add(styleClass);
            btn.setOnAction(e -> {
                Node row = btn.getParent();
                if (row != null) {
                    row.setDisable(true);
                } else {
                    btn.setDisable(true);
                }
                try {
                    if (action != null) {
                        action.run();
                    }
                } finally {
                    closeDialog();
                }
            });
            return btn;
        }

        private void closeDialog() {
            if (dialog != null) {
                dialog.close();
            }
        }
    }

    public enum Level {
        ERROR("#dc3545", SVGUtil.ERROR_ICON_PATH, "Error"),
        WARNING("#ffc107", SVGUtil.WARNING_ICON_PATH, "Warning"),
        INFO("#17a2b8", SVGUtil.INFO_ICON_PATH, "Information"),
        SUCCESS("#28a745", SVGUtil.SUCCESS_ICON_PATH, "Success");

        final String color;
        final String svgPath;
        final String title;

        Level(String color, String svgPath, String defaultTitle) {
            this.color = color;
            this.svgPath = svgPath;
            this.title = defaultTitle;
        }
    }
}