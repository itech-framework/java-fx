package io.github.itech_framework.java_fx.loader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.annotations.components.FxScope;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

final class OwnedProvider<T> implements FxProvider<T> {
    private static final Logger logger = LogManager.getLogger(OwnedProvider.class);
    private static final ThreadLocal<Deque<Class<?>>> CREATING = ThreadLocal.withInitial(ArrayDeque::new);

    private static final int MAX_NESTING = 16;
    
    private final Class<T> type;
    private final FxScope scope;
    private final ComponentRuntime runtime;
    private final int warnLimit;
    private final List<T> live = new ArrayList<>();
    private volatile T single;
    private boolean closed;
    private boolean warned;

    OwnedProvider(Class<T> type, FxScope scope, ComponentRuntime runtime, int warnLimit) {
        this.type = type;
        this.scope = scope;
        this.runtime = runtime;
        this.warnLimit = warnLimit;
    }

    @Override
    public T get() {
        requireOpen();
        if (scope == FxScope.PROTOTYPE) {
            return create();
        }
        T existing = single;
        if (existing != null) {
            return existing;
        }
        T created = create();
        T winner;
        synchronized (this) {
            if (single == null) {
                single = created;
                return created;
            }
            winner = single;
        }
        release(created);
        return winner;
    }

    @Override
    public T create(Object... constructorArgs) {
        requireOpen();
        Object[] args = constructorArgs == null ? new Object[0] : constructorArgs;
        T instance = runtime.onUiThread(() -> guardedLoad(args));
        boolean orphan;
        synchronized (this) {
            orphan = closed;
            if (!orphan) {
                live.add(instance);
                warnWhenTooManyAreAlive();
            }
        }
        if (orphan) {
            runtime.dispose(instance);
            throw ownerDisposed();
        }
        return instance;
    }

    @Override
    public void release(T instance) {
        if (instance == null) {
            return;
        }
        boolean tracked = false;
        synchronized (this) {
            for (Iterator<T> iterator = live.iterator(); iterator.hasNext();) {
                if (iterator.next() == instance) {
                    iterator.remove();
                    tracked = true;
                    break;
                }
            }
            if (single == instance) {
                single = null;
            }
        }
        if (tracked) {
            runtime.dispose(instance);
        }
    }
    
    @Override
    public void releaseAll() {
        List<T> pending;
        synchronized (this) {
            single = null;
            pending = new ArrayList<>(live);
            live.clear();
        }
        if (pending.isEmpty()) {
            return;
        }
        runtime.onUiThread(() -> {
            for (T instance : pending) {
                detach(instance);
            }
            return null;
        });
        for (int i = pending.size() - 1; i >= 0; i--) {
            try {
                runtime.dispose(pending.get(i));
            } catch (RuntimeException e) {
                logger.error("Failed to dispose {}", type.getName(), e);
            }
        }
    }

    private void detach(T instance) {
        try {
            Parent root = FxComponentLoader.rootOf(instance);
            if (root != null && root.getParent() instanceof Pane pane) {
                pane.getChildren().remove(root);
            }
        } catch (RuntimeException e) {
            logger.error("Failed to detach {}", type.getName(), e);
        }
    }

    @Override
    public synchronized int liveCount() {
        return live.size();
    }

    void disposeAll() {
        List<T> pending;
        synchronized (this) {
            closed = true;
            single = null;
            pending = new ArrayList<>(live);
            live.clear();
        }
        for (int i = pending.size() - 1; i >= 0; i--) {
            try {
                runtime.dispose(pending.get(i));
            } catch (RuntimeException e) {
                logger.error("Failed to dispose {}", type.getName(), e);
            }
        }
    }

    synchronized boolean limitWarningIssued() {
        return warned;
    }

    private T guardedLoad(Object[] args) {
        Deque<Class<?>> stack = CREATING.get();
        long nesting = stack.stream().filter(type::equals).count();
        if (nesting >= MAX_NESTING) {
            throw new FrameworkException(type.getSimpleName() + " is created inside its own initialization more than "
                    + MAX_NESTING + " levels deep. A component's @OnInit must stop creating components of its own type, "
                    + "and must not create a component that creates it again without an end.");
        }
        stack.push(type);
        try {
            return runtime.load(type, args);
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                CREATING.remove();
            }
        }
    }

    private synchronized void requireOpen() {
        if (closed) {
            throw ownerDisposed();
        }
    }

    private IllegalStateException ownerDisposed() {
        return new IllegalStateException("The owner of this provider for " + type.getName()
                + " was already disposed, so no more instances can be created.");
    }

    private void warnWhenTooManyAreAlive() {
        if (!warned && live.size() > warnLimit) {
            warned = true;
            logger.warn("{} instances of {} are alive at the same time (limit {}). Release instances you no longer "
                    + "use with FxProvider.release, or reuse one component instead of creating one per item.",
                    live.size(), type.getName(), warnLimit);
        }
    }
}