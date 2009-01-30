package net.sf.fisolator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.Callable;

/**
 * Created by Pavel Syrtsov
 * Date: Jan 29, 2009
 */
public class Call<T> implements Callable<T>, Runnable {
    public T call() throws Exception {
        run();
        return null;
    }

    public void run() {
        throw new RuntimeException("Call extending Call has to overwrite either method run() or call()");
    }
}
