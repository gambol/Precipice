/*
 * Copyright 2014 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.core.concurrent;

import net.uncontended.precipice.core.PrecipiceFunction;
import net.uncontended.precipice.core.Status;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Eventual<T> implements PrecipiceFuture<T>, Promise<T> {
    private volatile T result;
    private volatile Throwable exception;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private final AtomicReference<PrecipiceFunction<T, ?>> successCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<Throwable, ?>> errorCallback = new AtomicReference<>();
    private final AtomicReference<PrecipiceFunction<Void, ?>> timeoutCallback = new AtomicReference<>();

    @Override
    public boolean complete(T result) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                this.result = result;
                latch.countDown();
                PrecipiceFunction<T, ?> cb = successCallback.get();
                if (cb != null && successCallback.compareAndSet(cb, null)) {
                    cb.apply(result);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                this.exception = ex;
                latch.countDown();
                PrecipiceFunction<Throwable, ?> cb = errorCallback.get();
                if (cb != null && errorCallback.compareAndSet(cb, null)) {
                    cb.apply(ex);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean completeWithTimeout() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                latch.countDown();
                PrecipiceFunction<Void, ?> cb = timeoutCallback.get();
                if (cb != null && timeoutCallback.compareAndSet(cb, null)) {
                    cb.apply(null);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Future<T> future() {
        return this;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            if (result != null) {
                return result;
            } else {
                throw new ExecutionException(exception);
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public boolean isDone() {
        return status.get() != Status.PENDING;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancelling is not supported at this time");
    }

    @Override
    public <R> void onSuccess(PrecipiceFunction<T, R> fn) {
        if (status.get() == Status.SUCCESS) {
            fn.apply(result);
        } else {
            if (successCallback.compareAndSet(null, fn)
                    && status.get() == Status.SUCCESS
                    && successCallback.compareAndSet(fn, null)) {
                fn.apply(result);
            }
        }
    }

    @Override
    public <R> void onError(PrecipiceFunction<Throwable, R> fn) {
        if (status.get() == Status.ERROR) {
            fn.apply(exception);
        } else {
            if (errorCallback.compareAndSet(null, fn)
                    && status.get() == Status.ERROR
                    && errorCallback.compareAndSet(fn, null)) {
                fn.apply(exception);
            }
        }
    }

    @Override
    public <R> void onTimeout(PrecipiceFunction<Void, R> fn) {
        if (status.get() == Status.TIMEOUT) {
            fn.apply(null);
        } else {
            if (timeoutCallback.compareAndSet(null, fn)
                    && status.get() == Status.TIMEOUT
                    && timeoutCallback.compareAndSet(fn, null)) {
                fn.apply(null);
            }
        }
    }
}