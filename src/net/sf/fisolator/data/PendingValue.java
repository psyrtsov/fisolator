package net.sf.fisolator.data;

import java.util.concurrent.*;

/**
 * User: Pavel Syrtsov
 * Date: Oct 12, 2008
 * Time: 4:51:22 PM
 * todo: provide comments
 */
public class PendingValue<T> extends Semaphore implements Future<T> {
    volatile T value;

    public PendingValue() {
        super(0);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return super.availablePermits() > 0;
    }

    public T get() throws InterruptedException, ExecutionException {
        super.acquire();
        super.release();
        return value;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        super.acquire();
        super.release();
        return value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        super.release();
    }

    public void setValueWithoutRelease(T value) {
        this.value = value;
        super.release();
    }
}
