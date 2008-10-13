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

/**
 * todo: provide comments
 * User: Pavel Syrtsov
 * Date: Aug 24, 2008
 * Time: 10:37:56 AM
 */
public class AsyncFaultIsolator {
    private long startTime = System.currentTimeMillis();
    private List<TaskData> taskDataList = new ArrayList<TaskData>();
    private ExecutorService executor;

    public AsyncFaultIsolator(ExecutorService executor) {
        this.executor = executor;
    }

    public boolean invoke(final Runnable runnable, final FeatureFaultIsolator ... featureList) {
        if(!FaultIsolatorHelper.taskStart(featureList)) {
            // note that activeTaskPerFeatureCounter is not go to decrement
            // and feature data will be marked as unavailable
            return false;
        }
        Future<?> future = executor.submit(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    FaultIsolatorHelper.taskStopped(featureList);
                }
            }
        });
        TaskData taskData = new TaskData(future, featureList);
        taskDataList.add(taskData);
        return true;
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

    private void handleTimeOut() {
        for (TaskData taskData : taskDataList) {
            if (!taskData.future.isDone()) {
                 FaultIsolatorHelper.taskTimedOut(taskData.featureList);
            }
        }
    }

    private static class TaskData {
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