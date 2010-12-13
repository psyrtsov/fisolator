/**
 * Copyright 2008 Pavel Syrtsov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package net.sf.fisolator;

import java.util.concurrent.Callable;

/**
 * todo: provide comments
 * User: Pavel Syrtsov
 * Date: Aug 31, 2008
 * Time: 9:58:31 AM
 */
public interface ServiceTracker {
    boolean isAvailable();

    <T> void taskAccepted(Callable<T> callable);

    <T> T taskExec(Callable<T> callable) throws Exception;

    <T> void taskTimedOut(Callable<T> callable);
}
