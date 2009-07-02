package net.sf.fisolator.data;

import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * User: Pavel Syrtsov
 * Date: Oct 12, 2008
 * Time: 4:51:22 PM
 * todo: provide comments
 */
public class ValueFuture<T> extends Semaphore implements Future<T> {
    volatile T value;

    public ValueFuture() {
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

    public T get() throws InterruptedException {
        super.acquire();
        super.release();
        return value;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (!super.tryAcquire(timeout, unit)) {
            throw new TimeoutException();
        }
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
