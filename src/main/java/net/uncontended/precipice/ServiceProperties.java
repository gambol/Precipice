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

package net.uncontended.precipice;

import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.timeout.TimeoutService;

public class ServiceProperties {

    private ActionMetrics metrics = new DefaultActionMetrics();
    private CircuitBreaker breaker = new DefaultCircuitBreaker(new BreakerConfigBuilder().build());
    private TimeoutService timeoutService = TimeoutService.defaultTimeoutService;
    private PrecipiceSemaphore semaphore = null;
    private int concurrencyLevel = Service.MAX_CONCURRENCY_LEVEL;

    public void actionMetrics(ActionMetrics metrics) {
        this.metrics = metrics;
    }

    public void ircuitBreaker(CircuitBreaker breaker) {
        this.breaker = breaker;
    }

    public void timeoutService(TimeoutService timeoutService) {
        this.timeoutService = timeoutService;
    }

    public void concurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public void semaphore(PrecipiceSemaphore semaphore) {
        this.semaphore = semaphore;
    }
}
