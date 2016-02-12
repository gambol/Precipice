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

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.backpressure.BPRejectedException;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.backpressure.PromiseFactory;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.time.SystemTime;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadPoolServiceTest {

    @Mock
    private GuardRail<Status, Rejected> guardRail;
    @Mock
    private PromiseFactory<Status, Rejected> promiseFactory;

    private ThreadPoolService service;
    private ExecutorService executorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        executorService = PrecipiceExecutors.threadPoolExecutor("Test", 1, 100);
        service = new ThreadPoolService(executorService, guardRail, promiseFactory);

        when(guardRail.getClock()).thenReturn(new SystemTime());
    }

    @After
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void controllerAndThreadPoolShutdownWhenShutdownCallMade() {
        service.shutdown();

        verify(guardRail).shutdown();

        assertTrue(executorService.isShutdown());
    }

    @Test
    public void exceptionThrownIfControllerRejects() throws Exception {
        try {
            when(promiseFactory.acquirePermitsAndGetPromise(1L)).thenThrow(new BPRejectedException(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED));
            service.submit(TestCallable.success(), Long.MAX_VALUE);
            fail();
        } catch (BPRejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }

        try {
            when(promiseFactory.acquirePermitsAndGetPromise(1L)).thenThrow(new BPRejectedException(Rejected.CIRCUIT_OPEN));
            service.submit(TestCallable.success(), Long.MAX_VALUE);
            fail();
        } catch (BPRejectedException e) {
            assertEquals(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED, e.reason);
        }
    }

    @Test
    public void callableIsSubmittedAndRan() throws Exception {
        when(promiseFactory.acquirePermitsAndGetPromise(1L)).thenReturn(new Eventual<Status, Object>(1L));

        PrecipiceFuture<Status, String> f = service.submit(TestCallable.success(), 500);

        assertEquals("Success", f.get());
        assertEquals(Status.SUCCESS, f.getStatus());
    }

    @Test
    public void promisePassedToExecutorWillBeCompleted() throws Exception {
        PrecipicePromise<Status, String> promise = new Eventual<>(1L);

        when(promiseFactory.acquirePermitsAndGetPromise(1L, promise)).thenReturn(new Eventual<>(System.nanoTime(), promise));

        service.complete(TestCallable.success("Same Promise"), promise, TimeoutService.NO_TIMEOUT);

        verify(promiseFactory).acquirePermitsAndGetPromise(1L, promise);

        assertEquals("Same Promise", promise.future().get());
    }

    @Test
    public void promiseCanBeCompletedExternallyWithoutImpactingService() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        PrecipicePromise<Status, String> promise = new Eventual<>(1L);
        Eventual<Status, String> internalPromise = new Eventual<>(System.nanoTime(), promise);
        when(promiseFactory.acquirePermitsAndGetPromise(1L, promise)).thenReturn(internalPromise);

        service.complete(TestCallable.blocked(latch), promise, Long.MAX_VALUE);

        promise.complete(Status.SUCCESS, "CompleteOnThisThread");
        latch.countDown();

        assertEquals("CompleteOnThisThread", promise.future().get());
        assertEquals("Success", internalPromise.future().get());
    }

    @Test
    public void submittedCallableWillTimeout() throws Exception {
        when(promiseFactory.acquirePermitsAndGetPromise(1L)).thenReturn(new Eventual<Status, Object>(1L));

        CountDownLatch latch = new CountDownLatch(1);
        PrecipiceFuture<Status, String> future = service.submit(TestCallable.blocked(latch), 1);

        try {
            future.get();
            fail("Should have thrown ExecutionException from PrecipiceTimeoutException");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof PrecipiceTimeoutException);
        }

        assertEquals(Status.TIMEOUT, future.getStatus());
        assertTrue(future.error() instanceof PrecipiceTimeoutException);

        latch.countDown();
    }

    @Test
    public void erredCallableWillReturnException() {
        when(promiseFactory.acquirePermitsAndGetPromise(1L)).thenReturn(new Eventual<Status, Object>(1L));

        RuntimeException exception = new RuntimeException();
        PrecipiceFuture<Status, String> future = service.submit(TestCallable.erred(exception), 100);

        try {
            future.get();
            fail();
        } catch (InterruptedException e) {
            fail();
        } catch (ExecutionException e) {
            assertEquals(exception, e.getCause());
        }
        assertEquals(exception, future.error());
        assertNull(future.result());
        assertEquals(Status.ERROR, future.getStatus());
    }
}
