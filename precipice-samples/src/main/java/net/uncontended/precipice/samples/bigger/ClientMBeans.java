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

package net.uncontended.precipice.samples.bigger;

import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.metrics.counts.PartitionedCount;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.rejected.Rejected;
import net.uncontended.precipice.result.TimeoutableResult;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ClientMBeans {
    private final AtomicLong lastUpdateTimestamp = new AtomicLong(0);
    private volatile Map<Object, Object> currentMetrics;

    public ClientMBeans(String name, final Rolling<PartitionedCount<TimeoutableResult>> countMetrics) {

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new ExampleMetric() {
                @Override
                public long getTotal() {
                    return 0;
                }

                @Override
                public long getSuccesses() {
                    return 0;
                }

                @Override
                public long getErrors() {
                    return 0;
                }

                @Override
                public long getTimeouts() {
                    return 0;
                }

                @Override
                public long getMaxConcurrency() {
                    return 0;
                }

                @Override
                public long getAllRejected() {
                    return 0;
                }

                @Override
                public long getCircuitOpen() {
                    return 0;
                }

                private long getMetrics(String metric) {
//                    long currentTime = System.currentTimeMillis();
//                    long lastUpdateTime = lastUpdateTimestamp.get();
//                    if (currentTime - 1000 > lastUpdateTime && lastUpdateTimestamp.compareAndSet(lastUpdateTime, currentTime)) {
//                        currentMetrics = Snapshot.generate(countMetrics, countMetrics.metricCounters(1, TimeUnit.SECONDS));
//                    }

                    return 0L;
                }
            }, new ObjectName(String.format("net.uncontended.precipice:type=Service,name=%s", name)));

        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException |
                MalformedObjectNameException e) {
            e.printStackTrace();
        }

    }

    public ClientMBeans(String name, final Rolling<PartitionedCount<TimeoutableResult>> countMetrics, final DefaultCircuitBreaker<Rejected> breaker) {

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(new ExampleMetric() {
                @Override
                public long getTotal() {
                    return 0;
                }

                @Override
                public long getSuccesses() {
                    return 0;
                }

                @Override
                public long getErrors() {
                    return 0;
                }

                @Override
                public long getTimeouts() {
                    return 0;
                }

                @Override
                public long getMaxConcurrency() {
                    return 0;
                }

                @Override
                public long getAllRejected() {
                    return 0;
                }

                @Override
                public long getCircuitOpen() {
                    return 0;
                }

                private long getMetrics(String metric) {
//                    long currentTime = System.currentTimeMillis();
//                    long lastUpdateTime = lastUpdateTimestamp.get();
//                    if (currentTime - 1000 > lastUpdateTime && lastUpdateTimestamp.compareAndSet(lastUpdateTime, currentTime)) {
//                        currentMetrics = Snapshot.generate(countMetrics, countMetrics.metricCounters(1, TimeUnit.SECONDS));
//                    }

                    return (long) 0;
                }
            }, new ObjectName(String.format("net.uncontended.precipice:type=Service,name=%s", name)));

            ManagementFactory.getPlatformMBeanServer().registerMBean(new ExampleBreakerMetric() {
                @Override
                public boolean isOpen() {
                    return breaker.isOpen();
                }

                @Override
                public void forceOpen() {
                    breaker.forceOpen();
                }

                @Override
                public void forceClose() {
                    breaker.forceClosed();
                }
            }, new ObjectName(String.format("net.uncontended.precipice:type=CircuitBreaker,name=%s", name)));

        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException |
                MalformedObjectNameException e) {
            e.printStackTrace();
        }
    }

    @MXBean
    public interface ExampleMetric {
        long getTotal();

        long getSuccesses();

        long getErrors();

        long getTimeouts();

        long getMaxConcurrency();

        long getCircuitOpen();

        long getAllRejected();
    }

    @MXBean
    public interface ExampleBreakerMetric {
        boolean isOpen();

        void forceOpen();

        void forceClose();
    }


}
