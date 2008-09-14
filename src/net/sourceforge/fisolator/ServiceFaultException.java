package net.sourceforge.fisolator;

/**
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 4:12:14 PM
 * psdo: provide comments for class ${CLASSNAME}
 */
public class ServiceFaultException extends Exception {
    public ServiceFaultException() {
    }

    public ServiceFaultException(String message) {
        super(message);
    }

    public ServiceFaultException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceFaultException(Throwable cause) {
        super(cause);
    }
}
