package io.github.itech_framework.java_fx.ui.dialog;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.itech_framework.core.utils.DataStorageUtil;
import io.github.itech_framework.core.utils.validator.CommonValidator;
import io.github.itech_framework.java_fx.router.config.RouterConfig;
import io.github.itech_framework.java_fx.utils.FxResources;
import io.github.itech_framework.java_fx.utils.FxThread;
import io.github.itech_framework.java_fx.utils.node.StyleUtils;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public abstract class AbstractFxDialog {

    protected static final Duration ANIMATION_DURATION = Duration.millis(300);

    private static final int MAX_SHOW_RETRIES = 3;

    private enum State {
        NEW, SHOWING, SHOWN, CLOSING, CLOSED
    }

    protected final Stage dialogStage = new Stage();
    protected final StackPane rootPane = new StackPane();

    private final Stage owner;
    private final double blurRadius;
    private final DropShadow shadow = new DropShadow();
    private final CompletableFuture<Void> closedFuture = new CompletableFuture<>();

    private OwnerEffectGuard guard = OwnerEffectGuard.none();
    private volatile State state = State.NEW;
    private boolean closeRequested;
    private boolean closeOnEscape = true;
    private int showRetries;
    private double fromScale = 0.95;
    private double slideY = 20;

    protected AbstractFxDialog(Stage owner, double blurRadius) {
        this.owner = owner;
        this.blurRadius = blurRadius;

        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) {
            dialogStage.initOwner(owner);
        }

        rootPane.getStyleClass().add("dialog-root");
        rootPane.setPadding(new Insets(15));

        configureShadow(15, 0.2, 0.1);
        rootPane.setEffect(shadow);

        Scene scene = new Scene(rootPane);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (closeOnEscape && e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                requestClose(true);
            }
        });
        FxResources.applyTheme(scene.getStylesheets());
        dialogStage.setScene(scene);

        applyDarkModeStyle();

        dialogStage.setOnShown(e -> handleShown());
        dialogStage.setOnHidden(e -> finish());
    }

    @Deprecated
    protected AbstractFxDialog(Stage owner, double blurRadius, double cornerRadius) {
        this(owner, blurRadius);
    }

    protected final void configureShadow(double radius, double opacity, double spread) {
        shadow.setColor(Color.gray(0, opacity));
        shadow.setRadius(radius);
        shadow.setSpread(spread);
    }

    protected final void configureAnimation(double fromScale, double slideY) {
        this.fromScale = fromScale;
        this.slideY = slideY;
    }

    protected final void setCloseOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }

    protected void beforeShow() {
    }

    protected void afterClosed() {
    }

    public final void showAsync() {
        FxThread.run(() -> open(false));
    }

    public final void showAndWait() {
        if (Platform.isFxApplicationThread()) {
            open(true);
            return;
        }
        showAsync();
        closedFuture.join();
    }

    public final void close() {
        FxThread.run(() -> requestClose(true));
    }

    public final void closeImmediately() {
        FxThread.run(() -> requestClose(false));
    }

    public final boolean isShowing() {
        return state == State.SHOWN;
    }

    public void applyFonts(List<String> fontFamilies) {
        if (CommonValidator.validList(fontFamilies)) {
            StyleUtils.setFontFamily(dialogStage.getScene().getRoot(), fontFamilies);
        }
    }

    public void applyStylesheets(List<String> stylesheets) {
        if (CommonValidator.validList(stylesheets)) {
            List<String> existing = dialogStage.getScene().getStylesheets();
            for (String stylesheet : stylesheets) {
                if (!existing.contains(stylesheet)) {
                    existing.add(stylesheet);
                }
            }
        }
    }

    private void open(boolean wait) {
        if (state != State.NEW) {
            return;
        }
        state = State.SHOWING;
        try {
            beforeShow();
            guard = OwnerEffectGuard.apply(owner, blurRadius, ownerFill());
            rootPane.setOpacity(0);
            dialogStage.sizeToScene();
            if (wait) {
                dialogStage.showAndWait();
            } else {
                dialogStage.show();
            }
        } catch (IllegalStateException ex) {
            if (wait && !dialogStage.isShowing() && showRetries++ < MAX_SHOW_RETRIES) {
                guard.release();
                guard = OwnerEffectGuard.none();
                state = State.NEW;
                Platform.runLater(() -> open(true));
            } else {
                finish();
                throw ex;
            }
        } catch (RuntimeException ex) {
            finish();
            throw ex;
        }
    }

    private void handleShown() {
        if (state != State.SHOWING) {
            return;
        }
        state = State.SHOWN;
        if (closeRequested) {
            closeNow(false);
            return;
        }
        centerOnOwner();
        transition(0, 1, slideY, 0, fromScale, 1).play();
    }

    private void centerOnOwner() {
        if (owner == null || !owner.isShowing() || owner.isIconified()) {
            dialogStage.centerOnScreen();
            return;
        }
        dialogStage.setX(owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2);
        dialogStage.setY(owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2);
    }

    private void requestClose(boolean animate) {
        switch (state) {
        case NEW:
            finish();
            break;
        case SHOWING:
            closeRequested = true;
            break;
        case SHOWN:
            closeNow(animate);
            break;
        case CLOSING:
            if (!animate) {
                dialogStage.close();
            }
            break;
        default:
            break;
        }
    }

    private void closeNow(boolean animate) {
        state = State.CLOSING;
        if (animate) {
            Animation hide = transition(1, 0, 0, slideY, 1, fromScale);
            hide.setOnFinished(e -> dialogStage.close());
            hide.play();
        } else {
            dialogStage.close();
        }
    }

    private void finish() {
        if (state == State.CLOSED) {
            return;
        }
        state = State.CLOSED;
        try {
            guard.release();
            afterClosed();
        } finally {
            closedFuture.complete(null);
            if (dialogStage.isShowing()) {
                dialogStage.close();
            }
        }
    }

    private Animation transition(double fade0, double fade1, double y0, double y1, double scale0, double scale1) {
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, rootPane);
        fade.setFromValue(fade0);
        fade.setToValue(fade1);

        TranslateTransition translate = new TranslateTransition(ANIMATION_DURATION, rootPane);
        translate.setFromY(y0);
        translate.setToY(y1);

        ScaleTransition scale = new ScaleTransition(ANIMATION_DURATION, rootPane);
        scale.setFromX(scale0);
        scale.setFromY(scale0);
        scale.setToX(scale1);
        scale.setToY(scale1);

        return new ParallelTransition(fade, translate, scale);
    }

    private static Boolean darkMode() {
        Object value = DataStorageUtil.load(RouterConfig.getDarkModeKey());
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private void applyDarkModeStyle() {
        rootPane.getStyleClass().remove("dark-mode");
        if (Boolean.TRUE.equals(darkMode())) {
            rootPane.getStyleClass().add("dark-mode");
        }
    }

    private static Paint ownerFill() {
        Boolean dark = darkMode();
        if (dark == null) {
            return null;
        }
        return dark ? Color.BLACK : Color.WHITE;
    }
}