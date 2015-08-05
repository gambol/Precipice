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

package net.uncontended.precipice.core.pattern;


import net.uncontended.precipice.core.MultiService;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.Promise;
import net.uncontended.precipice.core.metrics.ActionMetrics;
import net.uncontended.precipice.core.metrics.DefaultActionMetrics;

import java.util.Map;

public class MultiLoadBalancer<C> extends AbstractPattern<C> implements MultiPattern<C> {

    private final SubmissionPattern<C> submissionBalancer;
    private final RunPattern<C> runBalancer;
    private final MultiService[] services;

    public MultiLoadBalancer(Map<MultiService, C> executorToContext, LoadBalancerStrategy strategy) {
        this(executorToContext, strategy, new DefaultActionMetrics());
    }

    @SuppressWarnings("unchecked")
    public MultiLoadBalancer(Map<MultiService, C> executorToContext, LoadBalancerStrategy strategy,
                             ActionMetrics metrics) {
        super(metrics);
        if (executorToContext.size() == 0) {
            throw new IllegalArgumentException("Cannot create load balancer with 0 Services.");
        }

        services = new MultiService[executorToContext.size()];
        C[] contexts = (C[]) new Object[executorToContext.size()];
        int i = 0;
        for (Map.Entry<MultiService, C> entry : executorToContext.entrySet()) {
            services[i] = entry.getKey();
            contexts[i] = entry.getValue();
            ++i;
        }
        this.submissionBalancer = new SubmissionLoadBalancer<>(services, contexts, strategy, metrics);
        this.runBalancer = new RunLoadBalancer<>(services, contexts, strategy, metrics);
    }

    @Override
    public <T> PrecipiceFuture<T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        return submissionBalancer.submit(action, millisTimeout);
    }

    @Override
    public <T> void complete(ResilientPatternAction<T, C> action, Promise<T> promise, long millisTimeout) {
        submissionBalancer.complete(action, promise, millisTimeout);
    }

    @Override
    public <T> T run(final ResilientPatternAction<T, C> action) throws Exception {
        return runBalancer.run(action);
    }

    @Override
    public void shutdown() {
        for (MultiService e : services) {
            e.shutdown();
        }
    }
}
