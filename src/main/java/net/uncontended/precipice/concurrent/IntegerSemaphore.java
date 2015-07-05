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

package net.uncontended.precipice.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

public class IntegerSemaphore implements PrecipiceSemaphore {

    private final AtomicInteger permitsRemaining;

    public IntegerSemaphore(int concurrencyLevel) {
        permitsRemaining = new AtomicInteger(concurrencyLevel);
    }

    @Override
    public boolean acquirePermit() {
        System.out.println(this);
        for (; ; ) {
            int permitsRemaining = this.permitsRemaining.get();
            if (permitsRemaining > 0) {
                if (this.permitsRemaining.compareAndSet(permitsRemaining, permitsRemaining - 1)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void releasePermit() {
        this.permitsRemaining.getAndIncrement();
    }
}
