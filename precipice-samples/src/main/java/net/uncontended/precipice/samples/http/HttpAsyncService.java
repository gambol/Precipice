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

package net.uncontended.precipice.samples.http;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import net.uncontended.precipice.Precipice;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.backpressure.PromiseFactory;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;

import java.util.concurrent.TimeoutException;


public class HttpAsyncService implements Precipice<Status, Rejected> {

    private final AsyncHttpClient client;
    private final GuardRail<Status, Rejected> guardRail;

    public HttpAsyncService(GuardRail<Status, Rejected> guardRail, AsyncHttpClient client) {
        this.guardRail = guardRail;
        this.client = client;
    }

    public PrecipiceFuture<Status, Response> submit(Request request) {
        final PrecipicePromise<Status, Response> promise = PromiseFactory.acquirePermitsAndGetPromise(guardRail, 1L);

        client.executeRequest(request, new AsyncCompletionHandler<Void>() {
            @Override
            public Void onCompleted(Response response) throws Exception {
                promise.complete(Status.SUCCESS, response);
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (t instanceof TimeoutException) {
                    promise.completeExceptionally(Status.TIMEOUT, t);
                } else {
                    promise.completeExceptionally(Status.ERROR, t);
                }
            }
        });
        return promise.future();
    }

    @Override
    public GuardRail<Status, Rejected> guardRail() {
        return guardRail;
    }


    public void shutdown() {
        shutdown(true);
    }

    public void shutdown(boolean shutdownClient) {
        guardRail.shutdown();
        if (shutdownClient) {
            client.close();
        }
    }
}
