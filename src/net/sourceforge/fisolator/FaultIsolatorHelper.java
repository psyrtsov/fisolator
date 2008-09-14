package net.sourceforge.fisolator;

/**
 * User: Pavel Syrtsov
 * Date: Aug 23, 2008
 * Time: 11:14:22 PM
 * psdo: provide comments for class ${CLASSNAME}
 */
public class FaultIsolatorHelper {

    public static boolean taskStart(FeatureFaultIsolator[] featureList) {
        // task can start if at least one feature using it is active
        for (FeatureFaultIsolator feature : featureList) {
            if (feature.isAvailable()) {
                // if at least one feature is not isolated we will kick off
                for (FeatureFaultIsolator featureToStart : featureList) {
                    featureToStart.taskStarted();
                }
                return true;
            }
        }
        // all feature are disable we shouldn't run thisk task
        return false;
    }

    public static void taskStopped(FeatureFaultIsolator[] featureList) {
        for (FeatureFaultIsolator feature : featureList) {
            feature.taskStopped();
        }
    }

    public static void taskTimedOut(FeatureFaultIsolator[] featureList) {
        for (FeatureFaultIsolator feature : featureList) {
            feature.taskTimedOut();
        }
    }
}
