package net.sf.fisolator.data;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;

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
    private transient ConcurrentMap<String, PendingValue> pendingValueMap;

    public PendingValueMap() {
        pendingValueMap = new ConcurrentHashMap<String, PendingValue>();
    }

    public <T> PendingValue<T> definePendingValue(String name) {
        PendingValue<T> newValue = new PendingValue<T>();
        //noinspection unchecked
        PendingValue<T> oldValue = pendingValueMap.putIfAbsent(name, newValue);
        return (oldValue == null)? newValue: oldValue;
    }

    public <T> PendingValue<T> get(String name) {
        //noinspection unchecked
        return pendingValueMap.get(name);
    }

    public <T> PendingValue<T> remove(String name) {
        //noinspection unchecked
        return pendingValueMap.remove(name);
    }
}
