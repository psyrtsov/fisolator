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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Pavel Syrtsov
 * Date: Aug 30, 2008
 * Time: 12:05:32 AM
 * todo: provide comments
 */
public class ServiceTrackerImpl implements ServiceTracker {
    AtomicInteger threadCounter = new AtomicInteger(0);
    private volatile boolean available = true;
    private int lockThreshold;
    private int unlockThreshold;

    public ServiceTrackerImpl(int lockThreshold, int unlockThreshold) {
        if (lockThreshold < unlockThreshold) {
            throw new RuntimeException("lockThreshold(" + lockThreshold + ") has to be higher then unlockThreshold(" + unlockThreshold + ")");
        }
        this.lockThreshold = lockThreshold;
        this.unlockThreshold = unlockThreshold;
    }

    public boolean isAvailable() {
        return available;
    }

    public <T> void taskAccepted(Callable<T> callable) {
        threadCounter.getAndIncrement();
    }

    public <T> T taskExec(Callable<T> callable) throws Exception {
        try {
            return callable.call();
        } finally {
            int activeCount = threadCounter.getAndDecrement();
            if (!available && ((activeCount - 1) <= unlockThreshold)) {
                available = true;
            }
        }
    }

    public <T> void taskTimedOut(Callable<T> callable) {
        int activeCount = threadCounter.get();
        if (available && (activeCount >= lockThreshold)) {
            available = false;
        }
    }

    public String toString() {
        return ServiceTrackerImpl.class.getSimpleName() + ": threadCounter=" + threadCounter + ", available=" + available;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public int getThreadCounter() {
        return threadCounter.get();
    }
}
