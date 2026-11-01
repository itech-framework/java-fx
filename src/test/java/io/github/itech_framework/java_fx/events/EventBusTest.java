package io.github.itech_framework.java_fx.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @AfterEach
    void clear() {
        EventBus.clearAll();
    }

    @Test
    void deliversToSubscribers() {
        List<Object> received = new ArrayList<>();
        EventBus.subscribe("greeting", received::add);
        EventBus.publish("greeting", "hello");
        assertEquals(List.of("hello"), received);
    }

    @Test
    void sameListenerIdReplacesThePreviousListener() {
        AtomicInteger calls = new AtomicInteger();
        EventBus.subscribe("tick", "id-1", data -> calls.addAndGet(10));
        EventBus.subscribe("tick", "id-1", data -> calls.addAndGet(1));
        EventBus.publish("tick", null);
        assertEquals(1, calls.get());
    }

    @Test
    void failingListenerDoesNotStopTheOthers() {
        AtomicInteger calls = new AtomicInteger();
        EventBus.subscribe("work", "bad", data -> {
            throw new IllegalStateException("boom");
        });
        EventBus.subscribe("work", "good", data -> calls.incrementAndGet());
        EventBus.publish("work", null);
        assertEquals(1, calls.get());
    }

    @Test
    void survivesConcurrentSubscribeAndPublish() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int worker = 0; worker < 8; worker++) {
            int id = worker;
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < 2000; i++) {
                    String listenerId = "w" + id + "-" + i;
                    EventBus.subscribe("load", listenerId, data -> {
                    });
                    EventBus.publish("load", i);
                    EventBus.unsubscribe("load", listenerId);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertEquals(0, EventBus.getListenerCount("load"));
    }
}