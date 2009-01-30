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

package net.sf;

import junit.framework.TestCase;

import java.util.concurrent.*;

import net.sf.fisolator.*;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 7:21:11 PM
 */
public class FaultIsolatorTest extends TestCase {
    private static final int LOCK_THRESHOLD = 4;
    private static final int UNLOCK_THRESHOLD = 2;
    private static final long TIMEOUT = 50L;
    private ExecutorService executor;

    private FeatureFaultIsolator feature1;
    private FeatureFaultIsolator feature2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        executor = Executors.newCachedThreadPool();
        feature1 = new FeatureFaultIsolatorImpl(LOCK_THRESHOLD, UNLOCK_THRESHOLD);
        feature2 = new FeatureFaultIsolatorImpl(LOCK_THRESHOLD, UNLOCK_THRESHOLD);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        executor.shutdownNow();
        executor = null;
        feature1 = null;
        feature2 = null;
    }

    public void testTimeout() throws Exception {
        FaultIsolator subject = new FaultIsolator(executor);
        try {
            subject.invoke(new Call() {
                public void run() {
                    try {
                        Thread.sleep(TIMEOUT + 100);
                    } catch (InterruptedException e) {
                        fail(e.getMessage());
                    }
                }
            }, TIMEOUT, feature1);
            fail("Expected timeout exception");
        } catch (ServiceFaultException e) {
            Throwable cause = e.getCause();
            assertEquals(TimeoutException.class, cause.getClass());
        }
    }

    public void testLockUnlock() throws Exception {
        final FaultIsolator subject = new FaultIsolator(executor);
        // startWaitingThreads it
        final Semaphore[] semaphoreList = startWaitingThreads(subject, LOCK_THRESHOLD, feature1);
        // verify
        checkLocked(subject, feature1);
        // unlock it
        int threadNumberToBeReleased = LOCK_THRESHOLD - UNLOCK_THRESHOLD + 1;
        for(int i=0; i<threadNumberToBeReleased; i++) {
            semaphoreList[i].release();
        }
        // we have to give some time for threads to finish unlocking
        Thread.sleep(50);
        // verify
        checkUnlocked(subject, feature1);
    }

    public void testSinglePointIsolation() throws Exception {
        FaultIsolator subject1 = new FaultIsolator(executor);
        // startWaitingThreads it
        final Semaphore[] semaphoreList = startWaitingThreads(subject1, LOCK_THRESHOLD+1, feature1);
        // verify that feature2 is locked
        FaultIsolator subject2 = new FaultIsolator(executor);
        checkLocked(subject2, feature1);
        // verify that not affected feature is not locked
        FaultIsolator subject3 = new FaultIsolator(executor);
        checkUnlocked(subject3, feature2);

        // unlock it
        int threadNumberToBeReleased = semaphoreList.length - UNLOCK_THRESHOLD + 1;
        for(int i=0; i<threadNumberToBeReleased; i++) {
            semaphoreList[i].release();
        }
        Thread.sleep(100L);
        // verify
        checkUnlocked(subject2, feature2);
    }

    private Semaphore[] startWaitingThreads(FaultIsolator subject, int waitThreadNum, FeatureFaultIsolator feature) throws Exception {
        final Semaphore semaphoreList[] = new Semaphore[waitThreadNum];
        // startWaitingThreads these features down
        for(int i=0; i< waitThreadNum; i++) {
            final Semaphore semaphore = new Semaphore(0);
            semaphoreList[i] = semaphore;
            startWaitingThread(subject, semaphore, feature);
        }
        // lockout should kick-in only on 1st timeout
        // make sure it didn't kick-in yet
        checkUnlocked(subject, feature);

        // we have to wait until 1st timeout will kick in to make lockout effective
        Thread.sleep(TIMEOUT + 50);
        return semaphoreList;
    }

    private void startWaitingThread(final FaultIsolator subject, final Semaphore semaphore, final FeatureFaultIsolator feature) {
        executor.submit(new Callable<Object>() {
            public Object call() throws Exception {
                return subject.invoke(new Callable<Object>() {
                    public Object call() throws Exception {
                        semaphore.acquire();
                        return null;
                    }
                }, TIMEOUT, feature);
            }
        });
    }

    private void checkLocked(FaultIsolator subject, FeatureFaultIsolator feature) throws ExecutionException, InterruptedException {
        try {
            subject.invoke(new Callable<Object>() {
                public Object call() throws Exception {
                    return null;
                }
            }, TIMEOUT, feature);
            fail("Expected invocation to be rejected due to lockout");
        } catch (ServiceFaultException e) {
            Throwable cause = e.getCause();
            assertNull(cause);
        }
    }

    private void checkUnlocked(FaultIsolator subject, FeatureFaultIsolator feature) throws Exception {
        final String dummyResult = "dummyResult";
        Object res = subject.invoke(new Callable<Object>() {
            public Object call() throws Exception {
                return dummyResult;
            }
        }, TIMEOUT, feature);
        assertSame(dummyResult, res);
    }
}
