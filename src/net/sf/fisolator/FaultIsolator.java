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

/**
 * This is the main class for fault isolation framework, it allows differnet types of calls to callable that to be isolated: <br/>
 * <li> enqueue methods - async execution of callable 
 * <li> invoke methods - sync execution with timeout
 * Created by: Pavel Syrtsov
 * Date: Aug 24, 2008
 * Time: 10:37:56 AM
 */
public class FaultIsolator {
    private long startTime = System.currentTimeMillis();
    private BlockingQueue<TaskData> taskDataList = new LinkedBlockingQueue<TaskData>();
    private ExecutorService executor;

    public FaultIsolator(ExecutorService executor) {
        this.executor = executor;
    }

    public long getStartTime() {
        return startTime;
    }

    public BlockingQueue<TaskData> getTaskDataList() {
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

    /**
     * This method intended to be used in combination with waitToComplete(), for details see javadoc for that method
     * @param callable - call to be executed on this service
     * @param service - service upon which we should count/track execution of this call
     * @param <T> - callable type
     * @return Future for this callable execution or null if service is locked out
     */
    public<T> Future enqueue(final Callable<T> callable, final ServiceTracker service) {
        if(!service.isAvailable()) {
            return null;
        }
        service.taskAccepted(callable);
        Future<T> future = executor.submit(new Callable<T>() {
            public T call() throws Exception {
                return service.taskExec(callable);
            }
        });
        TaskData taskData = new TaskData(callable, future, service);
        taskDataList.add(taskData);
        return taskData.future;
    }

    public <T> T invoke(final Callable<T> callable, long timeout, final ServiceTracker feature) throws ExecutionException, InterruptedException, ServiceFaultException {
        if (!feature.isAvailable()) {
            throw new ServiceFaultException("Feature " + feature + " is disabled");
        }
        feature.taskAccepted(callable);
        Future<T> future = executor.submit(new Callable<T>() {
            public T call() throws Exception {
                return feature.taskExec(callable);
            }
        });
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            feature.taskTimedOut(callable);
            throw new ServiceFaultException(e);
        }
    }

    public static <T> T invoke(final Callable<T> callable, ServiceTracker feature) throws Exception {
        if (!feature.isAvailable()) {
            throw new ServiceFaultException("Feature " + feature + " is disabled");
        }
        feature.taskAccepted(callable);
        return feature.taskExec(callable);
    }

    /**
     * Idea here is that you want to do bunch of calls in request and they can be done in parallel ,
     * but you can wait only so long for whole thing to be done. That's when you use this  method,
     * it allows to make sure that whole thing does not take longer than you can afford
     * and if it's done faster , then it returns immediately.
     *
     * This method intended to be used with single use FIsolator instance. It works like this:
     * 1) create new FIsolator (this is when clock is starting to tick)
     * 2) enqueue Callables to be executed
     * 3) call waitToComplete and handle timeout (usually just ignore it)
     * 4) call get on each feature,
     *    most likely with 0L argument since you already waited for time that you can afford to wait,
     *    if some of the gets throws timeout then gracefully degrade this one call and proceed with the rest
     * 5) throw this FIsolator away
     *
     * @param totalExecutionTime - defines how long to wait STARTING FROM MOMENT OF CREATION OF THIS FaultIsolator instance
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
                timeLeft = 0L;
            }
            try {
                taskData.future.get(timeLeft, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                if (handleTimeOut()) {
                    throw e;
                }
            }
        }
    }

    public boolean handleTimeOut() {
        boolean res = false;
        for (TaskData taskData : taskDataList) {
            if (!taskData.future.isDone()) {
                taskData.feature.taskTimedOut(taskData.callable);
                res = true;
            }
        }
        return res;
    }

    public static class TaskData {
        private final Callable<?> callable;
        private final Future<?> future;
        private final ServiceTracker feature;

        public TaskData(Callable<?> callable, Future<?> future, ServiceTracker feature) {
            this.callable = callable;
            this.future = future;
            this.feature = feature;
        }

        public Future<?> getFuture() {
            return future;
        }

        public ServiceTracker getFeature() {
            return feature;
        }


    }
}
