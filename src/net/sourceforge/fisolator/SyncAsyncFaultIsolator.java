package net.sourceforge.fisolator;

import java.util.concurrent.*;
import java.util.Arrays;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 7:05:44 PM
 * psdo: provide comments for class ${CLASSNAME}
 */
public class SyncAsyncFaultIsolator {
    private ExecutorService executor;
    private long timeout;
    private FeatureFaultIsolator[] featureList;

    public SyncAsyncFaultIsolator(ExecutorService executor, long timeout) {
        this.executor = executor;
        this.timeout = timeout;
    }

    public SyncAsyncFaultIsolator(ExecutorService executor, long timeout, FeatureFaultIsolator... featureList) {
        this(executor,timeout);
        this.featureList = featureList;
    }

    public <T> T invoke(final Callable<T> callable) throws ExecutionException, InterruptedException, ServiceFaultException {
        return invoke(callable, featureList);
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
