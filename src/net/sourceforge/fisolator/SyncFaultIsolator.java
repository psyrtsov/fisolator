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