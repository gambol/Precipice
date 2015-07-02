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

package net.uncontended.precipice.pattern;


import net.uncontended.precipice.MultiService;
import net.uncontended.precipice.ResilientCallback;
import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.concurrent.ResilientPromise;

import java.util.Map;

public class MultiLoadBalancer<C> implements MultiPattern<C> {

    private final SubmissionPattern<C> submissionBalancer;
    private final CompletionPattern<C> completionBalancer;
    private final RunPattern<C> runBalancer;
    private final MultiService[] services;

    @SuppressWarnings("unchecked")
    public MultiLoadBalancer(Map<MultiService, C> executorToContext, LoadBalancerStrategy strategy) {
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
        this.submissionBalancer = new SubmissionLoadBalancer<>(services, contexts, strategy);
        this.completionBalancer = new CompletionLoadBalancer<>(services, contexts, strategy);
        this.runBalancer = new RunLoadBalancer<C>(services, contexts, strategy);

    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientPatternAction<T, C> action, long millisTimeout) {
        return submissionBalancer.submit(action, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientPatternAction<T, C> action, ResilientCallback<T> callback,
                                         long millisTimeout) {
        return submissionBalancer.submit(action, callback, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      long millisTimeout) {
        completionBalancer.submitAndComplete(action, promise, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(final ResilientPatternAction<T, C> action, ResilientPromise<T> promise,
                                      ResilientCallback<T> callback, long millisTimeout) {
        completionBalancer.submitAndComplete(action, promise, callback, millisTimeout);

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