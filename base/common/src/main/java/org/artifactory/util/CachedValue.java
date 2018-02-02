package org.artifactory.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.artifactory.util.ArgUtil.checkValue;

/**
 * A utility for caching a single value.
 *
 * @author Yinon Avraham.
 */
public class CachedValue<T> {

    private static final Logger log = LoggerFactory.getLogger(CachedValue.class);

    private final Supplier<T> supplier;
    private final AtomicLong lastRefreshed = new AtomicLong(0);
    private final AtomicLong lastRead = new AtomicLong(0);
    private final Semaphore semaphore = new Semaphore(1);
    private volatile ValueWrapper<T> value;
    private T defaultValue;
    private AsyncTaskExecutor executor = new TaskExecutorAdapter(new SyncTaskExecutor()); //Synchronous executor
    private Long expiryAfterRefresh;
    private Long expiryAfterRead;
    private long initialLoadTimeout = 0;
    private long forceRefreshTimeout = TimeUnit.SECONDS.toMillis(10);
    private String name;

    private CachedValue(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier, "supplier is required");
    }

    /**
     * Get the cached value. (equivalent to <tt>get(false)</tt>)
     * @see #get(boolean)
     */
    public T get() {
        return get(false);
    }

    /**
     * Get the cached value, optionally forcing it to refresh.
     * @param force indicate whether to force the cache to refresh before returning the value
     * @see Builder#forceRefreshTimeout(long, TimeUnit)
     */
    public T get(boolean force) {
        if (value == null) {
            loadOrWait();
        } else {
            if (expired() || force) {
                refreshOrUseCached(force);
            }
        }
        lastRead.set(System.currentTimeMillis());
        if (value == null) {
            log.debug("[{}] Returning default value: {}", name, defaultValue);
            return defaultValue;
        }
        return value.getValue();
    }

    private void refreshOrUseCached(boolean force) {
        log.debug("[{}] Trying to acquire semaphore in order to refresh cached value (force={}).", name, force);
        long timeout = force ? forceRefreshTimeout : 0;
        if (tryAcquire(timeout)) {
            Future<?> future = assignValueAndReleaseSemaphore();
            if (timeout > 0) {
                waitFor(future, timeout);
            }
        } else {
            log.debug("[{}] Semaphore was not acquired, existing cached results will be returned.", name);
        }
    }

    private void loadOrWait() {
        if (tryAcquire(initialLoadTimeout)) {
            log.debug("[{}] Semaphore acquired.", name);
            if (value == null) {
                log.debug("[{}] Value not yet set.", name);
                //Since this is the first thread that populates the value - wait the initial loading timeout
                waitFor(assignValueAndReleaseSemaphore(), initialLoadTimeout);
            } else {
                semaphore.release();
                log.debug("[{}] Cache already set by another thread, no need to fetch it again.", name);
            }
        } else {
            log.debug("[{}] Semaphore was not acquired, reached timeout of {} millis.", name, initialLoadTimeout);
        }
    }

    private void waitFor(Future<?> future, long timeoutMillis) {
        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | TimeoutException e) {
            log.debug("[{}] Ignoring timeout/interruption: {}", name, e.toString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean tryAcquire(long timeoutMillis) {
        try {
            return semaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.debug("[{}] Trying to acquire semaphore got interrupted.", name, e);
            return false;
        }
    }

    private Future<?> assignValueAndReleaseSemaphore() {
        return executor.submit(() -> {
            try {
                log.debug("[{}] Getting and caching value.", name);
                value = new ValueWrapper<>(supplier.get());
                long now = System.currentTimeMillis();
                lastRefreshed.set(now);
                lastRead.set(now);
            } finally {
                semaphore.release();
            }
        });
    }

    private boolean expired() {
        long now = System.currentTimeMillis();
        return expiryAfterRead != null && now - lastRead.longValue() > expiryAfterRead ||
                expiryAfterRefresh != null && now - lastRefreshed.longValue() > expiryAfterRefresh;
    }

    @Override
    public String toString() {
        return name;
    }

    // --- Builder ---

    /**
     * Set the {@link Supplier} to use for loading the value to the cache
     * @param supplier the supplier to use
     * @param <T> the value type this {@link CachedValue} stores
     */
    public static <T> Builder<T> loadUsing(Supplier<T> supplier) {
        return new Builder<>(supplier);
    }

    public static class Builder<T> {

        private CachedValue<T> cachedValue;

        public Builder(Supplier<T> supplier) {
            this.cachedValue = new CachedValue<>(supplier);
        }

        /**
         * Set time to expire the cache after the value was last refreshed
         * @param duration the duration
         * @param unit the time unit
         */
        public Builder<T> expireAfterRefresh(long duration, TimeUnit unit) {
            long millis = unit.toMillis(duration);
            cachedValue.expiryAfterRefresh = checkValue(millis, ms -> ms >= 0, "expiry must be non-negative");
            return this;
        }

        /**
         * Set time to expire the cache after the value was last read
         * @param duration the duration
         * @param unit the time unit
         */
        public Builder<T> expireAfterRead(long duration, TimeUnit unit) {
            long millis = unit.toMillis(duration);
            cachedValue.expiryAfterRead = checkValue(millis, ms -> ms >= 0, "expiry must be non-negative");
            return this;
        }

        /**
         * Set the timeout for the initial value loading (default: 0)
         * @param duration timeout duration
         * @param unit time unit
         */
        public Builder<T> initialLoadTimeout(long duration, TimeUnit unit) {
            long millis = unit.toMillis(duration);
            cachedValue.initialLoadTimeout = checkValue(millis, ms -> ms >= 0, "timeout must be non-negative");
            return this;
        }

        /**
         * Set the timeout for forcing refresh (default: 10 seconds)
         * @param duration timeout duration
         * @param unit time unit
         */
        public Builder<T> forceRefreshTimeout(long duration, TimeUnit unit) {
            long millis = unit.toMillis(duration);
            cachedValue.forceRefreshTimeout = checkValue(millis, ms -> ms >= 0, "timeout must be non-negative");
            return this;
        }

        /**
         * Executor to use for asynchronous refresh
         * @param executor the executor
         */
        public Builder<T> async(AsyncTaskExecutor executor) {
            cachedValue.executor = requireNonNull(executor, "executor must be non-null");
            return this;
        }

        /**
         * Default value to return when a cached value is not yet set
         * @param defaultValue the default value
         */
        public Builder<T> defaultValue(T defaultValue) {
            cachedValue.defaultValue = defaultValue;
            return this;
        }

        /**
         * A name for the cached value
         * @param name the name
         */
        public Builder<T> name(String name) {
            cachedValue.name = name;
            return this;
        }

        public CachedValue<T> build() {
            validate();
            CachedValue<T> cachedValue = this.cachedValue;
            this.cachedValue = null;
            return cachedValue;
        }

        private void validate() {
            if (cachedValue.expiryAfterRefresh == null && cachedValue.expiryAfterRead == null) {
                throw new IllegalStateException("At least one is required: expireAfterRead / expireAfterRefresh");
            }
            if (cachedValue.expiryAfterRefresh != null && cachedValue.expiryAfterRead != null) {
                throw new IllegalStateException("Only one can be used: expireAfterRead / expireAfterRefresh");
            }
            if (StringUtils.isBlank(cachedValue.name)) {
                cachedValue.name = CachedValue.class.getSimpleName() + "@" + Integer.toHexString(cachedValue.hashCode());
            }
        }
    }

    private static class ValueWrapper<T> {
        private final T value;

        private ValueWrapper(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }
}
