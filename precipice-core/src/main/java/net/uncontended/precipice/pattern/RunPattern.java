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

import net.uncontended.precipice.RejectedActionException;
import net.uncontended.precipice.timeout.ActionTimeoutException;

public interface RunPattern<C> extends Pattern<C> {

    /**
     * Performs a {@link ResilientPatternAction} that will be run synchronously on the
     * calling thread. The result of the action will be returned.
     * <p/>
     * If the ResilientPatternAction throws a {@link ActionTimeoutException}, the result of
     * the action will be recorded as a timeout in the service's metrics. Any other exception
     * and the result of the action will be recorded as an error.
     *
     * @param action the action to run
     * @param <T>    the type of the result of the action
     * @return result of the action
     * @throws RejectedActionException if the action is rejected
     */
    <T> T run(ResilientPatternAction<T, C> action) throws Exception;
}