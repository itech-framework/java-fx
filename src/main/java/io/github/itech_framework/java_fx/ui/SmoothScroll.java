package io.github.itech_framework.java_fx.ui;

import java.util.ArrayList;
import java.util.List;

import io.github.itech_framework.java_fx.config.FxSettings;
import javafx.animation.AnimationTimer;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;

public final class SmoothScroll {

    private static final String STATE_KEY = SmoothScroll.class.getName() + ".state";
    private static final String EXCLUDE_KEY = SmoothScroll.class.getName() + ".exclude";
    private static final double EPS = 1e-6;
    private static final double SAME_VALUE = 1e-9;
    private static final int MAX_CACHE_SIZE = 4096;
    private static final List<State> ACTIVE = new ArrayList<>();
    private static final Ticker TICKER = new Ticker();
    private static final EventHandler<ScrollEvent> FILTER = SmoothScroll::onScroll;

    private static volatile boolean enabled = true;
    private static volatile double speed = 2.0;
    private static volatile double responseSeconds = 0.08;
    private static volatile boolean contentCaching = true;
    private static boolean installed;

    private SmoothScroll() {
    }

    public static void apply(FxSettings.Scroll settings) {
        setEnabled(settings.smooth());
        setSpeed(settings.speed());
        setResponseTime(settings.responseMillis() / 1000.0);
        setContentCaching(settings.contentCaching());
        if (settings.smooth()) {
            installGlobally();
        }
    }

    public static void installGlobally() {
        if (installed) {
            return;
        }
        installed = true;
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window window : change.getAddedSubList()) {
                        attach(window);
                    }
                }
            }
        });
        for (Window window : new ArrayList<>(Window.getWindows())) {
            attach(window);
        }
    }

    public static void install(ScrollPane pane) {
        pane.removeEventFilter(ScrollEvent.SCROLL, FILTER);
        pane.addEventFilter(ScrollEvent.SCROLL, FILTER);
    }

    public static void exclude(ScrollPane pane) {
        pane.getProperties().put(EXCLUDE_KEY, Boolean.TRUE);
    }

    public static void include(ScrollPane pane) {
        pane.getProperties().remove(EXCLUDE_KEY);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setSpeed(double value) {
        speed = Math.max(0.1, value);
    }

    public static void setResponseTime(double seconds) {
        responseSeconds = Math.max(0.01, seconds);
    }

    public static void setContentCaching(boolean value) {
        contentCaching = value;
    }

    private static void attach(Window window) {
        window.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(ScrollEvent.SCROLL, FILTER);
            }
            if (newScene != null) {
                newScene.removeEventFilter(ScrollEvent.SCROLL, FILTER);
                newScene.addEventFilter(ScrollEvent.SCROLL, FILTER);
            }
        });
        Scene scene = window.getScene();
        if (scene != null) {
            scene.removeEventFilter(ScrollEvent.SCROLL, FILTER);
            scene.addEventFilter(ScrollEvent.SCROLL, FILTER);
        }
    }

    private static void onScroll(ScrollEvent event) {
        if (!enabled || event.isConsumed() || event.isControlDown() || event.isInertia()
                || event.isDirect() || event.getTouchCount() > 0) {
            return;
        }
        double dx = event.getDeltaX();
        double dy = event.getDeltaY();
        if (dx == 0 && dy == 0) {
            return;
        }
        boolean horizontal = event.isShiftDown() || Math.abs(dx) > Math.abs(dy);
        double delta = horizontal ? (dx != 0 ? dx : dy) : dy;
        ScrollPane pane = find(event.getTarget(), horizontal, -delta);
        if (pane == null) {
            return;
        }
        state(pane).push(-delta * speed, horizontal);
        event.consume();
    }

    private static ScrollPane find(EventTarget target, boolean horizontal, double direction) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (node instanceof VirtualFlow || node instanceof TextInputControl) {
                return null;
            }
            if (node instanceof ScrollPane pane) {
                if (pane.getParent() instanceof TextInputControl) {
                    return null;
                }
                if (!pane.getProperties().containsKey(EXCLUDE_KEY)
                        && canScroll(pane, horizontal, direction)) {
                    return pane;
                }
            }
            node = node.getParent();
        }
        return null;
    }

    private static boolean canScroll(ScrollPane pane, boolean horizontal, double direction) {
        if (range(pane, horizontal) <= 0) {
            return false;
        }
        double value = horizontal ? pane.getHvalue() : pane.getVvalue();
        return direction > 0 ? value < 1 - EPS : value > EPS;
    }

    private static double range(ScrollPane pane, boolean horizontal) {
        Node content = pane.getContent();
        if (content == null) {
            return 0;
        }
        double total = horizontal
                ? content.getLayoutBounds().getWidth()
                : content.getLayoutBounds().getHeight();
        double view = horizontal
                ? pane.getViewportBounds().getWidth()
                : pane.getViewportBounds().getHeight();
        return total - view;
    }

    private static State state(ScrollPane pane) {
        Object found = pane.getProperties().get(STATE_KEY);
        if (found instanceof State existing) {
            return existing;
        }
        State created = new State(pane);
        pane.getProperties().put(STATE_KEY, created);
        return created;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Ticker extends AnimationTimer {

        private long last;
        private boolean running;

        void begin() {
            if (running) {
                return;
            }
            running = true;
            last = 0;
            start();
        }

        @Override
        public void handle(long now) {
            double dt = last == 0 ? 1.0 / 60 : Math.min((now - last) / 1e9, 0.05);
            last = now;
            double factor = 1 - Math.exp(-dt / responseSeconds);
            for (int i = ACTIVE.size() - 1; i >= 0; i--) {
                State state = ACTIVE.get(i);
                if (!enabled || !state.step(factor)) {
                    ACTIVE.remove(i);
                    state.finish();
                }
            }
            if (ACTIVE.isEmpty()) {
                running = false;
                stop();
            }
        }
    }

    private static final class State {

        private final ScrollPane pane;
        private double remainingH;
        private double remainingV;
        private double lastH = Double.NaN;
        private double lastV = Double.NaN;
        private boolean active;
        private Node cachedNode;
        private boolean previousCache;
        private CacheHint previousHint;

        State(ScrollPane pane) {
            this.pane = pane;
        }

        void push(double pixels, boolean horizontal) {
            double range = range(pane, horizontal);
            double value = horizontal ? pane.getHvalue() : pane.getVvalue();
            double last = horizontal ? lastH : lastV;
            double remaining = horizontal ? remainingH : remainingV;
            if (Double.isNaN(last) || Math.abs(value - last) > SAME_VALUE || remaining * pixels < 0) {
                remaining = 0;
            }
            remaining = clamp(remaining + pixels, -value * range, (1 - value) * range);
            if (horizontal) {
                remainingH = remaining;
                lastH = value;
            } else {
                remainingV = remaining;
                lastV = value;
            }
            if (!active) {
                active = true;
                begin();
                ACTIVE.add(this);
                TICKER.begin();
            }
        }

        boolean step(double factor) {
            remainingH = advance(true, remainingH, factor);
            remainingV = advance(false, remainingV, factor);
            return remainingH != 0 || remainingV != 0;
        }

        private double advance(boolean horizontal, double remaining, double factor) {
            if (remaining == 0) {
                return 0;
            }
            double range = range(pane, horizontal);
            double current = horizontal ? pane.getHvalue() : pane.getVvalue();
            double last = horizontal ? lastH : lastV;
            if (range <= 0 || Math.abs(current - last) > SAME_VALUE) {
                return 0;
            }
            boolean finalStep = Math.abs(remaining) < 1;
            double step = finalStep ? remaining : remaining * factor;
            double target = clamp(Math.rint(current * range + step) / range, 0, 1);
            if (horizontal) {
                pane.setHvalue(target);
            } else {
                pane.setVvalue(target);
            }
            double applied = horizontal ? pane.getHvalue() : pane.getVvalue();
            if (horizontal) {
                lastH = applied;
            } else {
                lastV = applied;
            }
            if (finalStep) {
                return 0;
            }
            double left = remaining - (applied - current) * range;
            if ((applied <= 0 && left < 0) || (applied >= 1 && left > 0)) {
                return 0;
            }
            return left;
        }

        private void begin() {
            Node content = pane.getContent();
            if (!contentCaching || content == null || content.isCache()) {
                return;
            }
            if (content.getLayoutBounds().getHeight() > MAX_CACHE_SIZE
                    || content.getLayoutBounds().getWidth() > MAX_CACHE_SIZE) {
                return;
            }
            cachedNode = content;
            previousCache = content.isCache();
            previousHint = content.getCacheHint();
            content.setCache(true);
            content.setCacheHint(CacheHint.SPEED);
        }

        void finish() {
            active = false;
            remainingH = 0;
            remainingV = 0;
            if (cachedNode != null) {
                cachedNode.setCache(previousCache);
                cachedNode.setCacheHint(previousHint);
                cachedNode = null;
            }
        }
    }
}