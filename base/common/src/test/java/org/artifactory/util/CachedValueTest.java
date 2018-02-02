package org.artifactory.util;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Lists;
import org.artifactory.test.TestUtils;
import org.artifactory.test.TestUtils.*;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.artifactory.test.TestUtils.*;
import static org.artifactory.util.CachedValueTest.SupplierBuilder.intSequenceSupplier;
import static org.artifactory.util.CachedValueTest.SupplierBuilder.supplier;
import static org.testng.Assert.assertEquals;

/**
 * @author Yinon Avraham.
 */
public class CachedValueTest {

    @BeforeClass
    public void setupClass() throws Exception {
        TestUtils.setLoggingLevel(CachedValue.class, Level.DEBUG);
    }

    @Test
    public void testConcurrentGetNonExpiringUsesSupplierOnce() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CachedValue<Integer> cachedValue = CachedValue
                .loadUsing(supplier(() -> 1).countUsing(counter).build())
                .expireAfterRead(10, TimeUnit.SECONDS)
                .initialLoadTimeout(1, TimeUnit.SECONDS)
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            List<Future<?>> futures = IntStream.range(0, 50)
                    .mapToObj(i ->
                            executor.submit(() -> {
                                cachedValue.get();
                                assertEquals(counter.get(), 1);
                            }))
                    .collect(Collectors.toList());
            joinFutures(futures);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testReadExpiry() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRead(300, TimeUnit.MILLISECONDS)
                .build();

        //expect 1st get to take ~500ms
        assertTiming(500, 50, () ->
            assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //expect 2nd get to be quick and not call the supplier
        assertTiming(25, 25, () ->
                assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //wait for 200ms, get should still be quick and not call the supplier (cache not yet expired)
        Thread.sleep(200);
        assertTiming(25, 25, () ->
                assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //wait for 200ms more, cache should not yet expire (expiry = 300ms from last read)
        Thread.sleep(200);
        assertTiming(25, 25, () ->
                assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //wait for 350ms since last read, cache should expire
        Thread.sleep(350);
        assertTiming(500, 50, () ->
                assertEquals((int)cachedValue.get(), 1)
        );
        assertEquals(counter.get(), 2);
    }

    @Test
    public void testRefreshExpiry() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                .build();

        //expect 1st get to take ~500ms
        assertTiming(500, 50, () ->
            assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //expect 2nd get to be quick and not call the supplier
        assertTiming(25, 25, () ->
                assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //wait for 200ms, get should still be quick and not call the supplier (cache not yet expired)
        Thread.sleep(200);
        assertTiming(25, 25, () ->
                assertEquals((int)cachedValue.get(), 0)
        );
        assertEquals(counter.get(), 1);
        //wait for 200ms more, cache should expire (expiry = 300ms from last refresh)
        Thread.sleep(200);
        assertTiming(500, 50, () ->
                assertEquals((int)cachedValue.get(), 1)
        );
        assertEquals(counter.get(), 2);
    }

    @Test
    public void testRefreshExpiryWithAsync() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        ExecutorService asyncExec = Executors.newFixedThreadPool(2);
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                .defaultValue(-1)
                .async(new TaskExecutorAdapter(asyncExec))
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            //3 threads, none should be blocked on the initial value loading, all shall return the default value
            List<? extends Future<?>> futures = IntStream.of(1, 2, 3)
                    .mapToObj(i -> executor.submit(() ->
                            assertTiming(25, 25, () ->
                                    assertEquals((int) cachedValue.get(), -1)
                            )
                    )).collect(Collectors.toList());
            joinFutures(futures);
            assertEquals(counter.get(), 0);
            assertEquals((int)cachedValue.get(), -1);
            //Wait a bit, the cache should not yet be populated
            Thread.sleep(200);
            assertEquals(counter.get(), 0);
            assertEquals((int)cachedValue.get(), -1);
            //Wait some more, now the cache should be populated
            Thread.sleep(350);
            assertEquals(counter.get(), 1);
            assertEquals((int)cachedValue.get(), 0);
            futures = IntStream.of(1, 2, 3)
                    .mapToObj(i -> executor.submit(() ->
                            assertTiming(25, 25, () ->
                                    assertEquals((int) cachedValue.get(), 0)
                            )
                    )).collect(Collectors.toList());
            joinFutures(futures);
            assertEquals(counter.get(), 1);
            //Wait a bit, the cache should expire but the cached value should be returned - the refresh is async
            Thread.sleep(350);
            assertTiming(25, 25, () -> assertEquals((int)cachedValue.get(), 0));
            assertEquals(counter.get(), 1);
            //Wait until the refresh is expected to be done
            Thread.sleep(550);
            assertTiming(25, 25, () -> assertEquals((int)cachedValue.get(), 1));
            assertEquals(counter.get(), 2);
        } finally {
            executor.shutdownNow();
            asyncExec.shutdownNow();
        }
    }

    @Test
    public void testInitialLoadingIsBlocking() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                .initialLoadTimeout(1000, TimeUnit.MILLISECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            //3 threads should be blocked on the initial value loading
            List<? extends Future<?>> futures = IntStream.of(1, 2, 3)
                    .mapToObj(i -> executor.submit(() ->
                            assertTiming(500, 50, () ->
                                    assertEquals((int) cachedValue.get(), 0)
                            )
                    )).collect(Collectors.toList());
            joinFutures(futures);
            assertEquals(counter.get(), 1);
            //After the value was loaded, all 3 thread should get it quickly from the cache.
            futures = IntStream.of(1, 2, 3)
                    .mapToObj(i -> executor.submit(() ->
                            assertTiming(25, 25, () ->
                                    assertEquals((int) cachedValue.get(), 0)
                            )
                    )).collect(Collectors.toList());
            joinFutures(futures);
            assertEquals(counter.get(), 1);
            //Wait for 350ms, the cache should expire. 1 thread should be blocked, the others should return the cached value.
            Thread.sleep(350);
            List<Future<TimedResult>> intFutures = Lists.newArrayList();
            for (int i = 0; i < 3; i++) {
                Future<TimedResult> future = executor.submit(() -> {
                    return callWithTiming(cachedValue::get);
                });
                intFutures.add(future);
            }
            List<TimedResult> results = assertTiming(500, 50, () -> joinFuturesAndGet(intFutures));
            assertEquals(counter.get(), 2);
            assertEquals(results.stream().map(TimedResult::getResult).sorted().collect(Collectors.toList()), Arrays.asList(0, 0, 1));
            List<Long> times = results.stream().map(TimedResult::getTime).sorted().collect(Collectors.toList());
            assertInRange(times.get(0), 0, 50);
            assertInRange(times.get(1), 0, 50);
            assertInRange(times.get(2), 450, 550);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testDefaultValueReturnedWhenInitialLoadingTimeoutReached() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                .initialLoadTimeout(300, TimeUnit.MILLISECONDS)
                .defaultValue(-1)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            //3 threads should be blocked on the initial value loading, 2 should reach timeout and get the default value
            List<Future<TimedResult>> futures = Lists.newArrayList();
            for (int i = 0; i < 3; i++) {
                Future<TimedResult> future = executor.submit(() -> {
                    return callWithTiming(cachedValue::get);
                });
                futures.add(future);
            }
            List<TimedResult> results = joinFuturesAndGet(futures);
            assertEquals(counter.get(), 1);
            results.sort(Comparator.comparingLong(TimedResult::getTime));
            assertInRange(results.get(0).getTime(), 250, 350);
            assertInRange(results.get(1).getTime(), 250, 350);
            assertInRange(results.get(2).getTime(), 450, 550);
            assertEquals((int)results.get(0).getResult(), -1);
            assertEquals((int)results.get(1).getResult(), -1);
            assertEquals((int)results.get(2).getResult(), 0);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testForceRefresh() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();
        CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                .forceRefreshTimeout(1000, TimeUnit.MILLISECONDS)
                .build();

        //expect:
        assertTiming(500, 50, () -> assertEquals((int)cachedValue.get(), 0));
        assertEquals(counter.get(), 1);
        assertTiming(25, 25, () -> assertEquals((int)cachedValue.get(), 0));
        assertEquals(counter.get(), 1);
        assertTiming(500, 50, () -> assertEquals((int)cachedValue.get(true), 1));
        assertEquals(counter.get(), 2);
    }

    @Test
    public void testForceRefreshWithTimeout() throws Exception {
        //given:
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> supplier = intSequenceSupplier(0, 1)
                .countUsing(counter)
                .withDelay(500, TimeUnit.MILLISECONDS)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            CachedValue<Integer> cachedValue = CachedValue.loadUsing(supplier)
                    .expireAfterRefresh(300, TimeUnit.MILLISECONDS)
                    .forceRefreshTimeout(200, TimeUnit.MILLISECONDS)
                    .async(new TaskExecutorAdapter(executor))
                    .defaultValue(-1)
                    .build();

            //expect:
            assertTiming(25, 25, () -> assertEquals((int) cachedValue.get(), -1));
            assertEquals(counter.get(), 0);
            Thread.sleep(550);
            assertTiming(25, 25, () -> assertEquals((int) cachedValue.get(), 0));
            assertEquals(counter.get(), 1);
            assertTiming(200, 50, () -> assertEquals((int) cachedValue.get(true), 0));
            assertEquals(counter.get(), 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOnlyOneExpiryTimeIsAccepted() throws Exception {
        CachedValue.loadUsing(() -> 0)
                .expireAfterRefresh(1, TimeUnit.SECONDS)
                .expireAfterRead(1, TimeUnit.SECONDS)
                .build();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExactlyOneExpiryTimeIsRequired() throws Exception {
        CachedValue.loadUsing(() -> 0).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExpireAfterRefreshMustBeNonNegative() throws Exception {
        CachedValue.loadUsing(() -> 0).expireAfterRefresh(-1, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExpireAfterReadMustBeNonNegative() throws Exception {
        CachedValue.loadUsing(() -> 0).expireAfterRead(-1, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInitialLoadTimeoutMustBeNonNegative() throws Exception {
        CachedValue.loadUsing(() -> 0).initialLoadTimeout(-1, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testForceRefreshTimeoutMustBeNonNegative() throws Exception {
        CachedValue.loadUsing(() -> 0).forceRefreshTimeout(-1, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testIfAsyncExecutorMustBeNonNull() throws Exception {
        CachedValue.loadUsing(() -> 0).async(null);
    }

    private void joinFutures(List<? extends Future<?>> futures) {
        futures.forEach(future -> {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> List<T> joinFuturesAndGet(List<? extends Future<T>> futures) {
        return futures.stream().map(this::joinFuture).collect(Collectors.toList());
    }

    private <T> T joinFuture(Future<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class SupplierBuilder<T> {
        private Supplier<T> supplier;

        public SupplierBuilder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public static <T> SupplierBuilder<T> supplier(Supplier<T> supplier) {
            return new SupplierBuilder<>(supplier);
        }

        public static SupplierBuilder<Integer> intSequenceSupplier(int start, int increment) {
            PrimitiveIterator.OfInt iterator = IntStream.iterate(start, i -> i += increment).iterator();
            return supplier(iterator::next);
        }

        public SupplierBuilder<T> countUsing(AtomicInteger counter) {
            supplier = new CountingSupplier<>(supplier, counter);
            return this;
        }

        public SupplierBuilder<T> withDelay(long duration, TimeUnit unit) {
            supplier = new DelayedSupplier<>(supplier, duration, unit);
            return this;
        }

        public Supplier<T> build() {
            return supplier;
        }

    }

    private static class CountingSupplier<T> implements Supplier<T> {

        private final AtomicInteger count;
        private final Supplier<T> supplier;

        private CountingSupplier(Supplier<T> supplier) {
            this(supplier, new AtomicInteger(0));
        }

        private CountingSupplier(Supplier<T> supplier, AtomicInteger count) {
            this.supplier = supplier;
            this.count = count;
        }

        public int getCount() {
            return count.get();
        }

        @Override
        public T get() {
            count.incrementAndGet();
            return supplier.get();
        }
    }

    private static class DelayedSupplier<T> implements Supplier<T> {
        private final Supplier<T> supplier;
        private final long duration;
        private final TimeUnit unit;

        private DelayedSupplier(Supplier<T> supplier, long duration, TimeUnit unit) {
            this.supplier = supplier;
            this.duration = duration;
            this.unit = unit;
        }

        @Override
        public T get() {
            try {
                Thread.sleep(unit.toMillis(duration));
            } catch (InterruptedException e) {
                //ignore
            }
            return supplier.get();
        }
    }
}