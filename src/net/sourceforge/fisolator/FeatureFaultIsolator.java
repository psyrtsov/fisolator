package net.sourceforge.fisolator;

/**
 * todo: provide comments for class ${CLASSNAME}
 * User: Pavel Syrtsov
 * Date: Aug 31, 2008
 * Time: 9:58:31 AM
 */
public interface FeatureFaultIsolator {
    void taskStarted();

    void taskStopped();

    void taskTimedOut();

    boolean isAvailable();
}
