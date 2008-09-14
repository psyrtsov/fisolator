package net.sourceforge.fisolator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Pavel Syrtsov
 * Date: Aug 30, 2008
 * Time: 12:05:32 AM
 * psdo: provide comments for class ${CLASSNAME}
 */
public class FeatureFaultIsolatorImpl implements FeatureFaultIsolator {
    AtomicInteger threadCounter = new AtomicInteger(0);
    private volatile boolean available = true;
    private int lockThreshold;
    private int unlockThreshold;

    public FeatureFaultIsolatorImpl(int lockThreshold, int unlockThreshold) {
        this.lockThreshold = lockThreshold;
        this.unlockThreshold = unlockThreshold;
    }

    public void taskStarted() {
        threadCounter.getAndIncrement();
    }

    public void taskStopped() {
        int activeCount = threadCounter.getAndDecrement();
        if (!available && ((activeCount - 1) <= unlockThreshold)) {
            available = true;
        }
    }

    public void taskTimedOut() {
        int activeCount = threadCounter.get();
        if (available && (activeCount >= lockThreshold)) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String toString() {
        return FeatureFaultIsolatorImpl.class.getSimpleName() + ": threadCounter=" + threadCounter + ", available=" + available;
    }
}
