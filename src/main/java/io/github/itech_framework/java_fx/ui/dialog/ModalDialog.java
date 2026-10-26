package io.github.itech_framework.java_fx.ui.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github.itech_framework.java_fx.utils.FxThread;
import io.github.itech_framework.java_fx.utils.node.ClickGuard;
import io.github.itech_framework.java_fx.utils.node.StyleUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ModalDialog extends AbstractFxDialog {

    private static final AtomicReference<ModalDialog> CURRENT = new AtomicReference<>();

    private final VBox container = new VBox(15);
    private final HBox buttonContainer = new HBox(10);

    public static ModalDialogBuilder builder() {
        return new ModalDialogBuilder();
    }

    private ModalDialog(Stage owner) {
    	super(owner, 10);
        container.setPadding(new Insets(20));
        container.getStyleClass().add("alert-container");
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        rootPane.getChildren().add(container);
    }

    @Override
    protected void beforeShow() {
        CURRENT.set(this);
    }

    @Override
    protected void afterClosed() {
        CURRENT.compareAndSet(this, null);
    }

    public void show() {
        showAndWait();
    }

    public static ModalDialog getCurrentDialog() {
        return CURRENT.get();
    }

    public static void setCurrentDialog(ModalDialog currentDialog) {
        CURRENT.set(currentDialog);
    }

    public static class ModalDialogBuilder {
        private final List<Node> contentNodes = new ArrayList<>();
        private final List<Button> buttons = new ArrayList<>();
        private final List<String> customFonts = new ArrayList<>();
        private final List<String> stylesheets = new ArrayList<>();
        private Stage owner;
        private ModalDialog dialog;

        public ModalDialogBuilder addContent(Node node) {
            contentNodes.add(node);
            return this;
        }

        public ModalDialogBuilder addButton(Button button) {
            buttons.add(button);
            return this;
        }

        public ModalDialogBuilder addActionButton(String text, Runnable action) {
            Button btn = new Button(text);
            btn.getStyleClass().add("dialog-action-button");
            ClickGuard.bind(btn, () -> {
                if (action != null) {
                    action.run();
                }
            });
            buttons.add(btn);
            return this;
        }

        public ModalDialogBuilder addAsyncActionButton(String text, Consumer<Runnable> action) {
            Button btn = new Button(text);
            btn.getStyleClass().add("dialog-action-button");
            ClickGuard.bind(btn, action);
            buttons.add(btn);
            return this;
        }

        public ModalDialogBuilder addCloseButton(String text) {
            Button btn = new Button(text);
            btn.getStyleClass().add("dialog-close-button");
            btn.setOnAction(e -> closeDialog());
            buttons.add(btn);
            return this;
        }

        public ModalDialogBuilder customFonts(String... fontFamilies) {
            if (fontFamilies != null) {
                Collections.addAll(customFonts, fontFamilies);
            }
            return this;
        }

        public ModalDialogBuilder stylesheets(String... stylesheets) {
            if (stylesheets != null) {
                Collections.addAll(this.stylesheets, stylesheets);
            }
            return this;
        }

        public ModalDialogBuilder addOwner(Stage owner) {
            this.owner = owner;
            return this;
        }

        public ModalDialog build() {
            ModalDialog created = FxThread.call(() -> {
                ModalDialog d = new ModalDialog(owner);
                d.container.getChildren().addAll(contentNodes);
                if (!buttons.isEmpty()) {
                    d.buttonContainer.getChildren().addAll(buttons);
                    d.container.getChildren().add(d.buttonContainer);
                }
                if (!customFonts.isEmpty()) {
                    StyleUtils.setFontFamily(d.rootPane, customFonts);
                }
                d.applyStylesheets(stylesheets);
                return d;
            });
            this.dialog = created;
            return created;
        }

        private void closeDialog() {
            if (dialog != null) {
                dialog.close();
            }
        }
    }
}