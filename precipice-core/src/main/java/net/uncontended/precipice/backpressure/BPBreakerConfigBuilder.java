/*
 * Copyright 2016 Timothy Brooks
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

package net.uncontended.precipice.backpressure;

public class BPBreakerConfigBuilder<Rejected extends Enum<Rejected>> {
    private final Rejected reason;
    public long trailingPeriodMillis = 5000;
    public long failureThreshold = Long.MAX_VALUE;
    public int failurePercentageThreshold = 50;
    public long healthRefreshMillis = 500;
    public long backOffTimeMillis = 1000;
    public long sampleSizeThreshold = 10;

    public BPBreakerConfigBuilder(Rejected reason) {
        this.reason = reason;
    }

    public BPBreakerConfigBuilder<Rejected> trailingPeriodMillis(long trailingPeriodMillis) {
        this.trailingPeriodMillis = trailingPeriodMillis;
        return this;
    }

    public BPBreakerConfigBuilder<Rejected> failureThreshold(long failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
    }

    public BPBreakerConfigBuilder<Rejected> failurePercentageThreshold(int failurePercentageThreshold) {
        this.failurePercentageThreshold = failurePercentageThreshold;
        return this;
    }

    public BPBreakerConfigBuilder<Rejected> backOffTimeMillis(long backOffTimeMillis) {
        this.backOffTimeMillis = backOffTimeMillis;
        return this;
    }

    public BPBreakerConfigBuilder<Rejected> healthRefreshMillis(long healthRefreshMillis) {
        this.healthRefreshMillis = healthRefreshMillis;
        return this;
    }

    public BPBreakerConfigBuilder<Rejected> sampleSizeThreshold(long sampleSizeThreshold) {
        this.sampleSizeThreshold = sampleSizeThreshold;
        return this;
    }

    public BPBreakerConfig<Rejected> build() {
        return new BPBreakerConfig<>(reason, failureThreshold, failurePercentageThreshold, trailingPeriodMillis,
                healthRefreshMillis, backOffTimeMillis, sampleSizeThreshold);
    }

}