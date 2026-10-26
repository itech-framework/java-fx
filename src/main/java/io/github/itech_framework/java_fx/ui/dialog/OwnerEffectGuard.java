package io.github.itech_framework.java_fx.ui.dialog;

import java.util.List;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

final class OwnerEffectGuard {

    private static final String KEY = "itech.owner.effect.state";

    private static final class State {
        int count;
        Effect effect;
        Paint fill;
    }

    private final Scene scene;
    private final Parent root;
    private boolean applied;

    private OwnerEffectGuard(Scene scene, Parent root) {
        this.scene = scene;
        this.root = root;
    }

    static OwnerEffectGuard none() {
        return new OwnerEffectGuard(null, null);
    }

    static OwnerEffectGuard apply(Stage owner, double blurRadius, Paint fallbackFill) {
        if (owner == null || blurRadius <= 0 || owner.getScene() == null) {
            return none();
        }
        Scene scene = owner.getScene();
        Parent root = scene.getRoot();
        State state = (State) root.getProperties().get(KEY);
        if (state == null) {
            state = new State();
            state.effect = root.getEffect();
            state.fill = scene.getFill();
            root.getProperties().put(KEY, state);
        }
        state.count++;
        root.setEffect(new GaussianBlur(blurRadius));

        Paint fill = backgroundPaint(root);
        if (fill == null) {
            fill = fallbackFill;
        }
        if (fill != null) {
            scene.setFill(fill);
        }
        OwnerEffectGuard guard = new OwnerEffectGuard(scene, root);
        guard.applied = true;
        return guard;
    }

    private static Paint backgroundPaint(Parent root) {
        if (root instanceof Region) {
            Background background = ((Region) root).getBackground();
            if (background != null && !background.getFills().isEmpty()) {
                List<BackgroundFill> fills = background.getFills();
                return fills.get(fills.size() - 1).getFill();
            }
        }
        return null;
    }

    void release() {
        if (!applied) {
            return;
        }
        applied = false;
        State state = (State) root.getProperties().get(KEY);
        if (state == null) {
            return;
        }
        state.count--;
        if (state.count <= 0) {
            root.setEffect(state.effect);
            scene.setFill(state.fill);
            root.getProperties().remove(KEY);
        }
    }
}