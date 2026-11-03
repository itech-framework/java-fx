package io.github.itech_framework.java_fx.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.core.exceptions.FrameworkException;
import io.github.itech_framework.java_fx.annotations.FxUse;
import io.github.itech_framework.java_fx.annotations.components.FxComponent;
import io.github.itech_framework.java_fx.annotations.components.FxScope;
import io.github.itech_framework.java_fx.annotations.components.Root;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

class FxUseTest {

    @FxComponent("")
    static class Widget {
    }

    static class Owner {
        @FxUse
        FxProvider<Widget> shared;

        @FxUse(scope = FxScope.PROTOTYPE)
        FxProvider<Widget> prototypes;
    }

    static class DirectOwner {
        @FxUse
        Widget widget;
    }

    static class PrototypeDirectOwner {
        @FxUse(scope = FxScope.PROTOTYPE)
        Widget widget;
    }

    static class FinalOwner {
        @FxUse
        private final FxProvider<Widget> provider = null;
    }

    @FxComponent("")
    static class Recursive {
        @FxUse(scope = FxScope.PROTOTYPE)
        FxProvider<Recursive> next;
    }
    
    @FxComponent("")
    static class Card {
        @Root
        Pane root = new Pane();
    }

    static class CardOwner {
        @FxUse(scope = FxScope.PROTOTYPE)
        FxProvider<Card> cards;
    }


    private static class FakeRuntime implements ComponentRuntime {
        final List<Object> created = new ArrayList<>();
        final List<Object> disposed = new ArrayList<>();

        @Override
        public <T> T onUiThread(Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T load(Class<T> type, Object[] constructorArgs) {
            try {
                Constructor<T> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                T instance = constructor.newInstance();
                created.add(instance);
                return instance;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void dispose(Object instance) {
            disposed.add(instance);
        }
    }

    private static final class RecursingRuntime extends FakeRuntime {
        private final int levels;
        private int count;

        RecursingRuntime(int levels) {
            this.levels = levels;
        }

        @Override
        public <T> T load(Class<T> type, Object[] constructorArgs) {
            Recursive instance = new Recursive();
            FxInjector.inject(instance, this, 1000);
            count++;
            if (count < levels) {
                instance.next.create();
            }
            return type.cast(instance);
        }
    }

    @Test
    void ownerScopeReturnsTheSameInstance() {
        FakeRuntime runtime = new FakeRuntime();
        Owner owner = new Owner();
        FxInjector.inject(owner, runtime, 100);
        assertSame(owner.shared.get(), owner.shared.get());
        assertEquals(1, runtime.created.size());
    }

    @Test
    void prototypeScopeCreatesANewInstanceEachTime() {
        FakeRuntime runtime = new FakeRuntime();
        Owner owner = new Owner();
        FxInjector.inject(owner, runtime, 100);
        assertNotSame(owner.prototypes.get(), owner.prototypes.get());
        assertEquals(2, owner.prototypes.liveCount());
    }

    @Test
    void releaseDestroysATrackedInstanceOnlyOnce() {
        FakeRuntime runtime = new FakeRuntime();
        Owner owner = new Owner();
        FxInjector.inject(owner, runtime, 100);
        Widget widget = owner.prototypes.create();
        owner.prototypes.release(widget);
        owner.prototypes.release(widget);
        assertEquals(1, runtime.disposed.size());
        assertEquals(0, owner.prototypes.liveCount());
    }

    @Test
    void disposingTheOwnerDestroysChildrenNewestFirstAndClosesTheProvider() {
        FakeRuntime runtime = new FakeRuntime();
        Owner owner = new Owner();
        FxInjector.inject(owner, runtime, 100);
        Widget first = owner.prototypes.get();
        Widget second = owner.prototypes.get();
        FxOwnership.disposeChildren(owner);
        assertEquals(List.of(second, first), runtime.disposed);
        assertThrows(IllegalStateException.class, () -> owner.prototypes.get());
    }

    @Test
    void directFieldIsLoadedWhenTheOwnerIsInjected() {
        FakeRuntime runtime = new FakeRuntime();
        DirectOwner owner = new DirectOwner();
        FxInjector.inject(owner, runtime, 100);
        assertNotNull(owner.widget);
        assertEquals(1, runtime.created.size());
    }

    @Test
    void directFieldRejectsPrototypeScope() {
        assertThrows(FrameworkException.class,
                () -> FxInjector.inject(new PrototypeDirectOwner(), new FakeRuntime(), 100));
    }

    @Test
    void finalFieldsAreRejected() {
        assertThrows(FrameworkException.class, () -> FxInjector.inject(new FinalOwner(), new FakeRuntime(), 100));
    }

    @Test
    void finiteRecursionIsAllowed() {
        RecursingRuntime runtime = new RecursingRuntime(5);
        OwnedProvider<Recursive> provider = new OwnedProvider<>(Recursive.class, FxScope.PROTOTYPE, runtime, 1000);
        provider.create();
        assertEquals(5, runtime.count);
    }

    @Test
    void endlessRecursionFails() {
        RecursingRuntime runtime = new RecursingRuntime(1000);
        OwnedProvider<Recursive> provider = new OwnedProvider<>(Recursive.class, FxScope.PROTOTYPE, runtime, 1000);
        assertThrows(FrameworkException.class, () -> provider.create());
    }
    

    @Test
    void releaseAllDetachesRootsAndDestroysEveryInstanceNewestFirst() {
        FakeRuntime runtime = new FakeRuntime();
        CardOwner owner = new CardOwner();
        FxInjector.inject(owner, runtime, 100);
        VBox parent = new VBox();
        Card first = owner.cards.create();
        Card second = owner.cards.create();
        parent.getChildren().addAll(first.root, second.root);
        owner.cards.releaseAll();
        assertEquals(0, parent.getChildren().size());
        assertEquals(List.of(second, first), runtime.disposed);
        assertEquals(0, owner.cards.liveCount());
    }

    @Test
    void providerStaysOpenAfterReleaseAll() {
        FakeRuntime runtime = new FakeRuntime();
        Owner owner = new Owner();
        FxInjector.inject(owner, runtime, 100);
        owner.shared.get();
        owner.shared.releaseAll();
        owner.shared.get();
        assertEquals(2, runtime.created.size());
        assertEquals(1, runtime.disposed.size());
    }
}