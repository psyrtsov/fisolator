package net.sf.fisolator;

import java.util.concurrent.*;

/**
 * Created by psyrtsov
 */
public class FaultIsolatorExecutor {
    ExecutorService executor;

    public FaultIsolatorExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public <T> Future<T> exec(final Callable<T> callable, final ServiceTracker feature) {
        if(!feature.isAvailable()) {
            return null;
        }
        feature.taskAccepted(callable);
        Future<T> future = executor.submit(new Callable<T>() {
            public T call() throws Exception {
                return feature.taskExec(callable);
            }
        });
        return new FeatureWrapper<T>(future, feature, callable);
    }


    private static class FeatureWrapper<T> implements Future<T> {
        private final Future<T> future;
        private final ServiceTracker feature;
        private final Callable<T> callable;

        public FeatureWrapper(Future<T> future, ServiceTracker feature, Callable<T> callable) {
            this.future = future;
            this.feature = feature;
            this.callable = callable;
        }

        public boolean cancel(boolean b) {
            return future.cancel(b);
        }

        public boolean isCancelled() {
            return future.isCancelled();
        }

        public boolean isDone() {
            return future.isDone();
        }

        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return future.get(l, timeUnit);
            } catch (TimeoutException e) {
                feature.taskTimedOut(callable);
                throw e;
            }
        }

    }
}
