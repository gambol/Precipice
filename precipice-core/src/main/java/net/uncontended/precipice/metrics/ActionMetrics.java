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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.SuperImpl;
import net.uncontended.precipice.SuperStatusInterface;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface ActionMetrics<T extends Enum<T> & SuperStatusInterface> {

    void incrementMetricCount(SuperImpl metric);

    void incrementMetricCount(SuperImpl metric, long nanoTime);

    long getMetricCount(SuperImpl metric);

    long getMetricCountForTotalPeriod(SuperImpl metric);

    long getMetricCountForTotalPeriod(SuperImpl metric, long nanoTime);

    long getMetricCountForTimePeriod(SuperImpl metric, long timePeriod, TimeUnit timeUnit);

    long getMetricCountForTimePeriod(SuperImpl metric, long timePeriod, TimeUnit timeUnit, long nanoTime);

    HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit);

    HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit, long nanoTime);

    Map<Object, Object> snapshot(long timePeriod, TimeUnit timeUnit);

    Iterable<MetricCounter<T>> metricCounterIterable(long timePeriod, TimeUnit timeUnit);

    MetricCounter<T> totalCountMetricCounter();
}
