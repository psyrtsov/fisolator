package net.sourceforge.fisolator;

import junit.framework.TestCase;

import java.util.concurrent.*;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 7:21:11 PM
 */
public class SyncAsyncFaultIsolatorTest extends TestCase {
    private static final int LOCK_THRESHOLD = 4;
    private static final int UNLOCK_THRESHOLD = 2;
    private static final long TIMEOUT = 50L;
    private ExecutorService executor;

    private FeatureFaultIsolator feature1;
    private FeatureFaultIsolator feature2;
    private FeatureFaultIsolator feature3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        executor = Executors.newCachedThreadPool();
        feature1 = new FeatureFaultIsolatorImpl(LOCK_THRESHOLD, UNLOCK_THRESHOLD);
        feature2 = new FeatureFaultIsolatorImpl(LOCK_THRESHOLD, UNLOCK_THRESHOLD);
        feature3 = new FeatureFaultIsolatorImpl(LOCK_THRESHOLD, UNLOCK_THRESHOLD);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        executor.shutdownNow();
        executor = null;
        feature1 = null;
        feature2 = null;
        feature3 = null;
    }

    public void testTimeout() throws Exception {
        SyncAsyncFaultIsolator subject = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature1, feature2);
        try {
            subject.invoke(new Callable<Object>() {
                public Object call() throws Exception {
                    Thread.sleep(TIMEOUT + 100);
                    return null;
                }
            });
            fail("Expected timeout exception");
        } catch (ServiceFaultException e) {
            Throwable cause = e.getCause();
            assertEquals(TimeoutException.class, cause.getClass());
        }
    }

    public void testLockUnlock() throws Exception {
        final SyncAsyncFaultIsolator subject = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature1);
        // lock it
        final Semaphore[] semaphoreList = lock(subject, LOCK_THRESHOLD);
        // verify
        checkLocked(subject);
        // unlock it
        int threadNumberToBeReleased = LOCK_THRESHOLD - UNLOCK_THRESHOLD + 1;
        for(int i=0; i<threadNumberToBeReleased; i++) {
            semaphoreList[i].release();
        }
        // we have to give some time for threads to finidh unlocking
        Thread.sleep(50);
        // verify
        checkUnlocked(subject);
    }

    public void testSinglePointIsolation() throws Exception {
        SyncAsyncFaultIsolator subject1 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature1, feature2);
        // lock it
        final Semaphore[] semaphoreList = lock(subject1, LOCK_THRESHOLD+1);
        // verify that feature2 is locked
        SyncAsyncFaultIsolator subject2 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature2);
        checkLocked(subject2);
        // verify that not affected feature is not locked
        SyncAsyncFaultIsolator subject3 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature3);
        checkUnlocked(subject3);

        // unlock it
        int threadNumberToBeReleased = semaphoreList.length - UNLOCK_THRESHOLD + 1;
        for(int i=0; i<threadNumberToBeReleased; i++) {
            semaphoreList[i].release();
        }
        Thread.sleep(100L);
        // verify
        checkUnlocked(subject2);
    }

    /**
     * here we are testing that single feature going to be locked out when it has threads stuck from different entry points
     * @throws Exception when test failed
     */
    public void testMultiplePointIsolation() throws Exception {
        SyncAsyncFaultIsolator subject1 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature1, feature2);
        // lock it
        final Semaphore[] semaphoreList1 = lock(subject1, UNLOCK_THRESHOLD);
        // verify that feature2 is locked
        SyncAsyncFaultIsolator subject2 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature2, feature3);
        final Semaphore[] semaphoreList2 = lock(subject2, LOCK_THRESHOLD - UNLOCK_THRESHOLD);

        // verify that feature2 is locked
        SyncAsyncFaultIsolator subject3 = new SyncAsyncFaultIsolator(executor, TIMEOUT, feature2);
        checkLocked(subject3);

        // unlock it
        int threadNumberToBeReleased = semaphoreList2.length;
        for(int i=0; i<threadNumberToBeReleased; i++) {
            semaphoreList2[i].release();
        }
        semaphoreList1[0].release();
        Thread.sleep(100L);
        // verify unlocked
        checkUnlocked(subject3);
    }

    private void checkLocked(SyncAsyncFaultIsolator subject) throws ExecutionException, InterruptedException {
        try {
            subject.invoke(new Callable<Object>() {
                public Object call() throws Exception {
                    return null;
                }
            });
            fail("Expected invocation to be rejected due to lockout");
        } catch (ServiceFaultException e) {
            Throwable cause = e.getCause();
            assertNull(cause);
        }
    }

    private Semaphore[] lock(SyncAsyncFaultIsolator subject, int lockCount) throws Exception {
        final Semaphore semaphoreList[] = new Semaphore[lockCount];
        // lock these features down
        for(int i=0; i< lockCount; i++) {
            final Semaphore semaphore = new Semaphore(0);
            semaphoreList[i] = semaphore;
            startWaiting(subject, semaphore);
        }
        // lockout should kick-in only on 1st timeout
        // make sure it didn't kick-in yet
        checkUnlocked(subject);

        // we have to wait until 1st timeout will kick in to make lockout effective
        Thread.sleep(TIMEOUT + 50);
        return semaphoreList;
    }

    private void startWaiting(final SyncAsyncFaultIsolator subject, final Semaphore semaphore) {
        executor.submit(new Callable<Object>() {
            public Object call() throws Exception {
                return subject.invoke(new Callable<Object>() {
                    public Object call() throws Exception {
                        semaphore.acquire();
                        return null;
                    }
                });
            }
        });
    }

    private void checkUnlocked(SyncAsyncFaultIsolator subject) throws Exception {
        final String dummyResult = "dummyResult";
        Object res = subject.invoke(new Callable<Object>() {
            public Object call() throws Exception {
                return dummyResult;
            }
        });
        assertSame(dummyResult, res);
    }
}
