package net.sf.fisolator.data;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * User: Pavel Syrtsov
 * Date: Oct 12, 2008
 * Time: 5:11:23 PM
 * todo: provide comments
 * todo: figure out how to associate pending value with running process so that
 * when user already submitted request for this value that user wouldn't
 * kickoff it again by hitting reload
 */
public class PendingValueMap implements Serializable {
    public static final long serialVersionUID = 1L;
    private transient ConcurrentMap<String, ValueFuture> pendingValueMap;

    public PendingValueMap() {
        pendingValueMap = new ConcurrentHashMap<String, ValueFuture>();
    }

    public <T> ValueFuture<T> definePendingValue(String name) {
        ValueFuture<T> newValueFuture = new ValueFuture<T>();
        //noinspection unchecked
        ValueFuture<T> oldValueFuture = pendingValueMap.putIfAbsent(name, newValueFuture);
        return (oldValueFuture == null) ? newValueFuture : oldValueFuture;
    }

    public <T> ValueFuture<T> get(String name) {
        //noinspection unchecked
        return pendingValueMap.get(name);
    }

    public <T> ValueFuture<T> remove(String name) {
        //noinspection unchecked
        return pendingValueMap.remove(name);
    }
}
