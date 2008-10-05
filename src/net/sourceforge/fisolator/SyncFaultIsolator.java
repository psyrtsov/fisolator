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

import java.util.concurrent.*;
import java.util.Arrays;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 7:05:44 PM
 * psdo: provide comments for class ${CLASSNAME}
 */
public class SyncFaultIsolator {

    public static <T> T invoke(final Callable<T> callable, FeatureFaultIsolator... featureList) throws Exception {
        if (!FaultIsolatorHelper.taskStart(featureList)) {
            throw new ServiceFaultException("Features " + Arrays.toString(featureList) + " are disabled");
        }
        try {
            return callable.call();
        } finally {
            FaultIsolatorHelper.taskStopped(featureList);
        }
    }

    public static void invoke(final Runnable runnable, FeatureFaultIsolator... featureList) throws ServiceFaultException {
        if (!FaultIsolatorHelper.taskStart(featureList)) {
            throw new ServiceFaultException("Features " + Arrays.toString(featureList) + " are disabled");
        }
        try {
            runnable.run();
        } finally {
            FaultIsolatorHelper.taskStopped(featureList);
        }
    }
}