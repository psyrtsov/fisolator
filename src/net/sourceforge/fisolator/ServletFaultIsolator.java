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
