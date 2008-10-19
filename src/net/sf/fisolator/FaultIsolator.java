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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * todo: provide comments
 * User: Pavel Syrtsov
 * Date: Aug 24, 2008
 * Time: 10:37:56 AM
 */
public class FaultIsolator {
    private long startTime = System.currentTimeMillis();
    private List<TaskData> taskDataList = new ArrayList<TaskData>();
    private ExecutorService executor;

    public FaultIsolator(ExecutorService executor) {
        this.executor = executor;
    }

    public long getStartTime() {
        return startTime;
    }

    public List<TaskData> getTaskDataList() {
        return taskDataList;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Future enqueue(final Runnable runnable, final FeatureFaultIsolator ... featureList) {
        return doEnqueue(new Callable<Object>() {
            public Object call() throws Exception {
                try {
                    runnable.run();
                    return null;
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        }, featureList);
    }

    public<T> Future enqueue(final Callable<T> callable, final FeatureFaultIsolator ... featureList) {
        return doEnqueue(new Callable<T>() {
            public T call() throws Exception {
                try {
                    return callable.call();
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        }, featureList);
    }

    private<T> Future doEnqueue(final Callable<T> callable, final FeatureFaultIsolator ... featureList) {
        if(!FaultIsolatorHelper.taskStart(featureList)) {
            return null;
        }
        Future<T> future = executor.submit(callable);
        TaskData taskData = new TaskData(future, featureList);
        taskDataList.add(taskData);
        return taskData.future;
    }

    public <T> T invoke(final Callable<T> callable, long timeout, final FeatureFaultIsolator... featureList) throws ExecutionException, InterruptedException, ServiceFaultException {
        return doInvoke(new Callable<T>() {
            public T call() throws Exception {
                try {
                    return callable.call();
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        }, timeout, featureList);
    }

    public void invoke(final Runnable runnable, long timeout, final FeatureFaultIsolator... featureList) throws ExecutionException, InterruptedException, ServiceFaultException {
        doInvoke(new Callable<Object>() {
            public Object call() throws Exception {
                try {
                    runnable.run();
                    return null;
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        }, timeout, featureList);
    }

    private <T> T doInvoke(final Callable<T> callable, long timeout, final FeatureFaultIsolator... featureList) throws ExecutionException, InterruptedException, ServiceFaultException {
        if (!FaultIsolatorHelper.taskStart(featureList)) {
            throw new ServiceFaultException("Features " + Arrays.toString(featureList) + " are disabled");
        }
        Future<T> future = executor.submit(callable);
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            FaultIsolatorHelper.taskTimedOut(featureList);
            throw new ServiceFaultException(e);
        }
    }

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

    /**
     * todo: there got to be better name for this method
     * @param totalExecutionTime - defines how long to wait STARTING FROM MOMENT OF CREATION OF THIS OBJECT
     * @throws ExecutionException - exception that had been generated by Runnable submitted before
     * @throws TimeoutException - wait for all Runnables to finish timed out
     * @throws InterruptedException - wait had been interrupted
     */
    public void waitToComplete(long totalExecutionTime) throws ExecutionException, TimeoutException, InterruptedException {
        for (TaskData taskData : taskDataList) {
            if (taskData.future.isDone()) {
                continue;
            }
            long timeLeft = totalExecutionTime - (System.currentTimeMillis() - startTime);
            if (timeLeft <= 0L) {
                throw new TimeoutException();
            }
            try {
                taskData.future.get(timeLeft, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                handleTimeOut();
                throw e;
            }
        }
    }

    public void handleTimeOut() {
        for (TaskData taskData : taskDataList) {
            if (!taskData.future.isDone()) {
                 FaultIsolatorHelper.taskTimedOut(taskData.featureList);
            }
        }
    }

    public static class TaskData {
        private final Future<?> future;
        private final FeatureFaultIsolator[] featureList;

        public TaskData(Future<?> future, FeatureFaultIsolator[] featureList) {
            this.future = future;
            this.featureList = featureList;
        }

        public Future<?> getFuture() {
            return future;
        }

        public FeatureFaultIsolator[] getFeatureList() {
            return featureList;
        }


    }
}
