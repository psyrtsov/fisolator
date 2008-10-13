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

import java.util.concurrent.*;
import java.util.Arrays;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 7:05:44 PM
 * todo: provide comments
 */
public class SyncAsyncFaultIsolator {
    private ExecutorService executor;
    private long timeout;

    public SyncAsyncFaultIsolator(ExecutorService executor, long timeout) {
        this.executor = executor;
        this.timeout = timeout;
    }

    public <T> T invoke(final Callable<T> callable, final FeatureFaultIsolator... featureList) throws ExecutionException, InterruptedException, ServiceFaultException {
        if (!FaultIsolatorHelper.taskStart(featureList)) {
            throw new ServiceFaultException("Features " + Arrays.toString(featureList) + " are disabled");
        }
        Future<T> future = executor.submit(new Callable<T>() {
            public T call() throws Exception {
                try {
                    return callable.call();
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        });
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            FaultIsolatorHelper.taskTimedOut(featureList);
            throw new ServiceFaultException(e);
        }
    }
}
