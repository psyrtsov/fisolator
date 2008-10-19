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

package net.sf.fisolator.http;

import net.sf.fisolator.FaultIsolator;

import javax.servlet.ServletRequest;
import java.util.concurrent.*;

/**
 * todo: provide comments
 * User: Pavel Syrtsov
 * Date: Sep 1, 2008
 * Time: 10:01:07 AM
 */
public class ServletFaultIsolator {
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    private static ExecutorService executor = DEFAULT_EXECUTOR;

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void setExecutor(ExecutorService executor) {
        ServletFaultIsolator.executor = executor;
    }

    /**
     * create AsyncFaultIsolator, one per request
     * @param servletRequest - AsyncFaultIsolator is stored in this request
     * @return created instance
     */
    public static FaultIsolator createFaultIsolator(ServletRequest servletRequest) {
        FaultIsolator fi = getFaultIsolatorIfExists(servletRequest);
        if (fi == null) {
            fi = new FaultIsolator(executor);
            servletRequest.setAttribute(FaultIsolator.class.getName(), fi);
        }
        return fi;
    }

    public static FaultIsolator getFaultIsolatorIfExists(ServletRequest servletRequest) {
        return (FaultIsolator) servletRequest.getAttribute(FaultIsolator.class.getName());
    }
}
