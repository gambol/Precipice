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

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class RoundRobinLoadBalancerTest {

    @Test
    public void wrappingWorks() {
        int start = (Integer.MAX_VALUE / 2) - 1;
        PatternStrategy strategy = new RoundRobinLoadBalancer(3, 3, new AtomicInteger(start));

        assertEquals(start % 3, strategy.nextIndices()[0]);
        assertEquals((start + 1) % 3, strategy.nextIndices()[0]);
        assertEquals(0, strategy.nextIndices()[0]);
        assertEquals(1, strategy.nextIndices()[0]);
        assertEquals(2, strategy.nextIndices()[0]);
    }
}
