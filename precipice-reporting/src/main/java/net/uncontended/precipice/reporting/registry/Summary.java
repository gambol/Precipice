/*
 * Copyright 2015 Timothy Brooks
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
 */

package net.uncontended.precipice.reporting.registry;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.PartitionedCount;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.WritableCountMetrics;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Summary<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {
    public final Class<Result> resultClazz;
    public final Class<Rejected> rejectedClazz;
    private final GuardRail<Result, Rejected> guardRail;

    private final long[] totalResultCounts;
    private final long[] resultCounts;

    private final long[] totalRejectedCounts;
    private final long[] rejectedCounts;

    private long currentStartEpoch;
    private long currentEndEpoch;

    private volatile int current = -1;
    private final Slice<Result, Rejected>[] slices;

    private final SummaryProperties properties;

    public Summary(SummaryProperties properties, GuardRail<Result, Rejected> guardRail) {
        this.guardRail = guardRail;

        resultClazz = guardRail.getResultMetrics().getMetricClazz();
        int resultLength = resultClazz.getEnumConstants().length;
        totalResultCounts = new long[resultLength];
        resultCounts = new long[resultLength];

        rejectedClazz = guardRail.getRejectedMetrics().getMetricClazz();
        int rejectedLength = rejectedClazz.getEnumConstants().length;
        totalRejectedCounts = new long[rejectedLength];
        rejectedCounts = new long[rejectedLength];

        slices = (Slice<Result, Rejected>[]) new Slice[properties.bufferSize];
        for (int i = 0; i < slices.length; ++i) {
            slices[i] = new Slice<>(resultClazz, rejectedClazz);
        }
        this.properties = properties;
    }

    public void refresh() {
        refresh(guardRail.getClock().currentTimeMillis(), guardRail.getClock().nanoTime());
    }

    public void refresh(long epochTime, long nanoTime) {
        Arrays.fill(resultCounts, 0);
        Arrays.fill(rejectedCounts, 0);

        WritableCountMetrics<Result> resultMetrics = guardRail.getResultMetrics();
        WritableCountMetrics<Rejected> rejectedMetrics = guardRail.getRejectedMetrics();

        long localStartEpoch = Long.MAX_VALUE;
        long localEndEpoch = 0L;

        if (resultMetrics instanceof Rolling) {
            Rolling<PartitionedCount<Result>> rollingMetrics = (Rolling<PartitionedCount<Result>>) resultMetrics;
            IntervalIterator<PartitionedCount<Result>> intervals = rollingMetrics.intervals(nanoTime);
            PartitionedCount<Result> interval;
            while (intervals.hasNext()) {
                interval = intervals.next();
                long startDiffMillis = TimeUnit.NANOSECONDS.toMillis(intervals.intervalStart());
                long endDiffMillis = TimeUnit.NANOSECONDS.toMillis(intervals.intervalEnd());
                long startEpoch = startDiffMillis + epochTime;
                if (startEpoch >= currentEndEpoch && endDiffMillis != 0) {
                    for (Result t : resultClazz.getEnumConstants()) {
                        resultCounts[t.ordinal()] += interval.getCount(t);
                    }
                    localStartEpoch = Math.min(localStartEpoch, startEpoch);
                    localEndEpoch = Math.max(localEndEpoch, endDiffMillis + epochTime);
                }
            }
        }

        if (rejectedMetrics instanceof Rolling) {
            Rolling<PartitionedCount<Rejected>> rollingMetrics = (Rolling<PartitionedCount<Rejected>>) rejectedMetrics;
            IntervalIterator<PartitionedCount<Rejected>> intervals = rollingMetrics.intervals();
            PartitionedCount<Rejected> interval;
            while (intervals.hasNext()) {
                interval = intervals.next();
                long relativeStart = intervals.intervalStart();
                long relativeEnd = intervals.intervalEnd();
                if (relativeStart >= currentEndEpoch && relativeEnd != 0) {
                    for (Rejected t : rejectedClazz.getEnumConstants()) {
                        rejectedCounts[t.ordinal()] += interval.getCount(t);
                    }
                    localStartEpoch = Math.min(localStartEpoch, relativeStart);
                    localEndEpoch = Math.max(localEndEpoch, relativeEnd);
                }
            }
        }

        if (properties.accumulateTotalResults) {
            for (Result t : resultClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalResultCounts[metricIndex] += resultCounts[metricIndex];
            }
        } else {
            for (Result t : resultClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
//                totalResultCounts[metricIndex] = resultMetrics.getCount(t);
            }
        }

        if (properties.accumulateTotalRejections) {
            for (Rejected t : rejectedClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalRejectedCounts[metricIndex] += rejectedCounts[metricIndex];
            }
        } else {
            for (Rejected t : rejectedClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
//                totalRejectedCounts[metricIndex] = rejectedMetrics.getCount(t);
            }
        }
        currentStartEpoch = localStartEpoch;
        currentEndEpoch = localEndEpoch;
        updateSlice();
        ++current;

    }

    public Slice<Result, Rejected> currentSlice() {
        int current = this.current;
        if (current == -1) {
            return slices[0];
        }
        return slices[current % slices.length];
    }

    public Slice<Result, Rejected>[] getSlices() {
        return slices;
    }

    private void updateSlice() {
        Slice<Result, Rejected> slice = slices[current + 1];

        slice.startEpoch = currentStartEpoch;
        slice.endEpoch = currentEndEpoch;
        System.arraycopy(resultCounts, 0, slice.resultCounts, 0, resultCounts.length);
        System.arraycopy(totalResultCounts, 0, slice.totalResultCounts, 0, totalResultCounts.length);
        System.arraycopy(rejectedCounts, 0, slice.rejectedCounts, 0, rejectedCounts.length);
        System.arraycopy(totalRejectedCounts, 0, slice.totalRejectedCounts, 0, totalRejectedCounts.length);
    }
}
