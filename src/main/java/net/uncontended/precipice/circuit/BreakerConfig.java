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

package net.uncontended.precipice.circuit;

public class BreakerConfig {

    public final int failurePercentageThreshold;
    public final long failureThreshold;
    public final long trailingPeriodMillis;
    public final long healthRefreshMillis;
    public final long backOffTimeMillis;

    public BreakerConfig(long failureThreshold, int failurePercentageThreshold, long trailingPeriodMillis,
                         long healthRefreshMillis, long backOffTimeMillis) {
        this.failureThreshold = failureThreshold;
        this.failurePercentageThreshold = failurePercentageThreshold;
        this.trailingPeriodMillis = trailingPeriodMillis;
        this.healthRefreshMillis = healthRefreshMillis;
        this.backOffTimeMillis = backOffTimeMillis;
    }

}
