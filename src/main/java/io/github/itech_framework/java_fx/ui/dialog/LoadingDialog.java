package io.github.itech_framework.java_fx.ui.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import io.github.itech_framework.java_fx.utils.FxThread;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoadingDialog extends AbstractFxDialog {

    private static final AtomicReference<LoadingDialog> CURRENT = new AtomicReference<>();

    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label messageLabel = new Label("Loading...");
    private final VBox container = new VBox(20);
    private Label progressTextLabel;

    private LoadingDialog(Stage owner) {
    	super(owner, 8);
    	setCloseOnEscape(false);
    	configureShadow(10, 0.15, 0.05);
    	configureAnimation(0.9, 0);
    	dialogStage.setResizable(false);
        configureShadow(10, 0.15, 0.05);
        configureAnimation(0.9, 0);
        dialogStage.setResizable(false);

        progressIndicator.setMaxSize(60, 60);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressIndicator.getStyleClass().add("loading-progress");

        messageLabel.getStyleClass().add("loading-message");

        VBox contentBox = new VBox(15, progressIndicator, messageLabel);
        contentBox.setAlignment(Pos.CENTER);

        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);
        container.getChildren().add(contentBox);
        container.getStyleClass().add("loading-container");

        rootPane.getChildren().add(container);
    }

    public static LoadingDialogBuilder builder() {
        return new LoadingDialogBuilder();
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
        showAsync();
    }

    public void updateMessage(String message) {
        FxThread.run(() -> messageLabel.setText(message));
    }

    public void updateProgress(double progress) {
        FxThread.run(() -> {
            if (progress < 0) {
                progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                if (progressTextLabel != null) {
                    progressTextLabel.setText("");
                }
                return;
            }
            double value = Math.min(1, progress);
            progressIndicator.setProgress(value);
            if (progressTextLabel != null) {
                progressTextLabel.setText(Math.round(value * 100) + "%");
            }
        });
    }

    public static LoadingDialog getCurrentLoadingDialog() {
        return CURRENT.get();
    }

    public static LoadingDialog showLoading(Stage owner) {
        LoadingDialog dialog = builder().owner(owner).build();
        dialog.show();
        return dialog;
    }

    public static LoadingDialog showLoading(Stage owner, String message) {
        LoadingDialog dialog = builder().owner(owner).message(message).build();
        dialog.show();
        return dialog;
    }

    public static CompletableFuture<Void> runAsync(Stage owner, String message, Runnable work) {
        LoadingDialog dialog = showLoading(owner, message);
        return CompletableFuture.runAsync(work).whenComplete((result, error) -> dialog.close());
    }

    public static class LoadingDialogBuilder {
        private Stage owner;
        private String message = "Loading...";
        private ProgressType progressType = ProgressType.INDETERMINATE;
        private final List<String> customFonts = new ArrayList<>();
        private boolean showProgressText;
        private double size = 60.0;

        public LoadingDialogBuilder owner(Stage owner) {
            this.owner = owner;
            return this;
        }

        public LoadingDialogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public LoadingDialogBuilder progressType(ProgressType progressType) {
            this.progressType = progressType;
            return this;
        }

        public LoadingDialogBuilder customFonts(String... fontFamilies) {
            if (fontFamilies != null) {
                for (String font : fontFamilies) {
                    if (!customFonts.contains(font)) {
                        customFonts.add(font);
                    }
                }
            }
            return this;
        }

        public LoadingDialogBuilder showProgressText(boolean showProgressText) {
            this.showProgressText = showProgressText;
            return this;
        }

        public LoadingDialogBuilder size(double size) {
            this.size = size;
            return this;
        }

        public LoadingDialog build() {
            return FxThread.call(() -> {
                LoadingDialog dialog = new LoadingDialog(owner);
                dialog.messageLabel.setText(message);

                if (progressType == ProgressType.DETERMINATE) {
                    dialog.progressIndicator.setProgress(0);
                }

                dialog.progressIndicator.setPrefSize(size, size);
                dialog.progressIndicator.setMaxSize(size, size);

                if (showProgressText) {
                    Label text = new Label(progressType == ProgressType.DETERMINATE ? "0%" : "");
                    text.getStyleClass().add("loading-progress-text");
                    VBox.setMargin(text, new Insets(5, 0, 0, 0));
                    dialog.progressTextLabel = text;
                    dialog.container.getChildren().add(text);
                }

                dialog.applyFonts(customFonts);
                return dialog;
            });
        }
    }

    public enum ProgressType {
        DETERMINATE,
        INDETERMINATE
    }
}