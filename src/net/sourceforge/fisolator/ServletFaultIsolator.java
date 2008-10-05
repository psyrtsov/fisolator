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

package net.sourceforge.fisolator;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;

/**
 * psdo: provide comments for class ${CLASSNAME}
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 10:01:07 AM
 */
public class ServletFaultIsolator {
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    private static ExecutorService executor = DEFAULT_EXECUTOR;

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void setExecutor(ExecutorService executor) {
        ServletFaultIsolator.executor = executor;
    }

    public static AsyncFaultIsolator getAsyncFaultIsolator(HttpServletRequest httpServletRequest) {
        AsyncFaultIsolator fi = (AsyncFaultIsolator) httpServletRequest.getAttribute(AsyncFaultIsolator.class.getName());
        if (fi == null) {
            fi = new AsyncFaultIsolator(executor);
            httpServletRequest.setAttribute(AsyncFaultIsolator.class.getName(), fi);
        }
        return fi;
    }

    public static SyncAsyncFaultIsolator createSyncAsyncFaultIsolator(long timeout) {
        return new SyncAsyncFaultIsolator(executor, timeout);
    }

    public static boolean featureIsAvailable(FeatureFaultIsolator feature, HttpServletRequest httpServletRequest) {
        AsyncFaultIsolator fi = (AsyncFaultIsolator) httpServletRequest.getAttribute(AsyncFaultIsolator.class.getName());
        AtomicInteger featurePerActionCounter = fi.getCounter(feature);
        return featurePerActionCounter.get() <= 0;
    }


}
