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

import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.metrics.counts.WritableCounts;
import net.uncontended.precipice.pattern.Pattern;
import net.uncontended.precipice.pattern.WritableSequence;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.semaphore.PrecipiceSemaphore;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.timeout.DelayQueueTimeoutService;
import net.uncontended.precipice.timeout.Timeout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class ThreadPoolPatternTest {

    private Object context1 = new Object();
    private Object context2 = new Object();
    private Object context3 = new Object();
    @Mock
    private ThreadPoolService service1;
    @Mock
    private ThreadPoolService service2;
    @Mock
    private ThreadPoolService service3;
    @Mock
    private GuardRail<TimeoutableResult, ?> guardRail1;
    @Mock
    private GuardRail<TimeoutableResult, ?> guardRail2;
    @Mock
    private GuardRail<TimeoutableResult, ?> guardRail3;
    @Mock
    private ExecutorService executor1;
    @Mock
    private ExecutorService executor2;
    @Mock
    private ExecutorService executor3;
    @Mock
    private DelayQueueTimeoutService timeoutService1;
    @Mock
    private DelayQueueTimeoutService timeoutService2;
    @Mock
    private DelayQueueTimeoutService timeoutService3;
    @Mock
    private GuardRail<TimeoutableResult, PatternRejected> guardRail;
    @Mock
    private Clock clock;
    @Mock
    private PrecipiceSemaphore semaphore;
    @Mock
    private WritableCounts<TimeoutableResult> resultCounts;
    @Mock
    private WritableCounts<PatternRejected> rejectedCounts;
    @Mock
    private Pattern<TimeoutableResult, ThreadPoolService<?>> pattern;
    @Mock
    private PatternCallable<String, Object> action;
    @Captor
    private ArgumentCaptor<CancellableTask<TimeoutableResult, ?>> task1Captor;
    @Captor
    private ArgumentCaptor<CancellableTask<TimeoutableResult, ?>> task2Captor;
    @Captor
    private ArgumentCaptor<ThreadPoolTimeout> timeout1Captor;
    @Captor
    private ArgumentCaptor<ThreadPoolTimeout> timeout2Captor;

    private ThreadPoolPattern<Object> poolPattern;
    private long submitTimeNanos = 10L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Map<ThreadPoolService<?>, Object> services = new LinkedHashMap<>();
        services.put(service1, context1);
        services.put(service2, context2);
        services.put(service3, context3);

        when(service1.guardRail()).thenReturn(guardRail1);
        when(service2.guardRail()).thenReturn(guardRail2);
        when(service3.guardRail()).thenReturn(guardRail3);
        when(service1.getExecutor()).thenReturn(executor1);
        when(service2.getExecutor()).thenReturn(executor2);
        when(service3.getExecutor()).thenReturn(executor3);
        when(service1.getTimeoutService()).thenReturn(timeoutService1);
        when(service2.getTimeoutService()).thenReturn(timeoutService2);
        when(service3.getTimeoutService()).thenReturn(timeoutService3);

        when(guardRail.getClock()).thenReturn(clock);
        when(guardRail.getResultCounts()).thenReturn(resultCounts);
        when(guardRail.getRejectedCounts()).thenReturn(rejectedCounts);
        when(clock.nanoTime()).thenReturn(submitTimeNanos);

        when(action.call(context1)).thenReturn("Service1");
        when(action.call(context2)).thenReturn("Service2");
        when(action.call(context3)).thenReturn("Service3");

        this.poolPattern = new ThreadPoolPattern<>(services, guardRail, pattern);
    }

    @Test
    public void actionsSubmittedToServices() throws Exception {
        WritableSequence<ThreadPoolService<?>> iterable = prepIterable(service1, service3);
        long millisTimeout = 100L;

        when(guardRail.acquirePermits(1L)).thenReturn(null);
        when(pattern.getPrecipices(1L, submitTimeNanos)).thenReturn(iterable);
        when(guardRail1.acquirePermits(1L, submitTimeNanos)).thenReturn(null);
        when(guardRail3.acquirePermits(1L, submitTimeNanos)).thenReturn(null);

        PrecipiceFuture<TimeoutableResult, String> f = poolPattern.submit(action, millisTimeout);

        verifyZeroInteractions(service2);
        verify(executor1).execute(task1Captor.capture());
        verify(executor3).execute(task2Captor.capture());
        verify(timeoutService1).scheduleTimeout(any(Timeout.class), eq(millisTimeout), eq(10L));
        verify(timeoutService3).scheduleTimeout(any(Timeout.class), eq(millisTimeout), eq(10L));

        CancellableTask<TimeoutableResult, ?> task1 = task1Captor.getValue();
        CancellableTask<TimeoutableResult, ?> task2 = task1Captor.getValue();

        assertNull(f.getResult());
        task1.run();
        task2.run();
        assertEquals(TimeoutableResult.SUCCESS, f.getResult());
        assertEquals("Service1", f.getValue());
    }

    @Test
    public void ifNoServiceReturnedThenAllRejected() throws Exception {
        WritableSequence<ThreadPoolService<?>> iterable = prepIterable();
        long millisTimeout = 100L;

        when(guardRail.acquirePermits(1L, submitTimeNanos)).thenReturn(null);
        when(pattern.getPrecipices(1L, submitTimeNanos)).thenReturn(iterable);

        try {
            poolPattern.submit(action, millisTimeout);
            fail("Should have been rejected");
        } catch (RejectedException e) {
            assertEquals(PatternRejected.ALL_REJECTED, e.reason);
        }

        verify(guardRail).releasePermitsWithoutResult(1, submitTimeNanos);
        verify(rejectedCounts).write(PatternRejected.ALL_REJECTED, 1L, submitTimeNanos);

        verifyZeroInteractions(service1);
        verifyZeroInteractions(service2);
        verifyZeroInteractions(service3);
    }

    private static WritableSequence<ThreadPoolService<?>> prepIterable(ThreadPoolService... services) {
        WritableSequence<ThreadPoolService<?>> iterable = new WritableSequence<>(services.length);

        for (ThreadPoolService<?> service : services) {
            iterable.add(service);
        }
        return iterable;
    }
}
