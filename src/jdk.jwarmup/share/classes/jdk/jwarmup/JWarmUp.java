/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 Alibaba Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jwarmup;
import java.security.BasicPermission;
/**
 * If the CompilationWarmUpOptimistic is off, application should
 * implicitly notify jvm that startup of application is done
 */
public class JWarmUp {

    @SuppressWarnings("serial")
    public static class JWarmUpPermission extends BasicPermission {
      public JWarmUpPermission(String s) {
        super(s);
      }
    }

    /**
     * Returns the singleton WhiteBox instance.
     *
     * The returned JWarmUp object should be carefully guarded
     * by the caller, since it can be used to read and write data
     * at arbitrary memory addresses. It must never be passed to
     * untrusted code.
     */
    public synchronized static JWarmUp getJWarmUp() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new JWarmUpPermission("getInstance"));
        }
        return instance;
    }


    /* Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();

    // only instance allowed
    private static JWarmUp instance = new JWarmUp();

    static {
        registerNatives();
    }

    private boolean hasNotified = false;

    // prevent created outside jdk.jwarmup
    private JWarmUp() {}

    /**
     * Notify jvm that application startup is done.
     * <p>
     * Should be explicitly call after startup of application if
     * CompilationWarmUpOptimistic is off. Otherwise, it does
     * nothing and just prints a warning message.
     *
     * @version 1.8
     */
    public synchronized void notifyApplicationStartUpIsDone() {
        if (!hasNotified) {
            hasNotified = true;
            notifyApplicationStartUpIsDone0();
        }
    }

    /**
     * Notify jvm to deoptimize warmup methods
     * <p>
     * Should be explicitly call after startup of application 
     * and warmup compilation is completed
     * vm option CompilationWarmUpExplicitDeopt must be on
     * Otherwise, it does nothing and just prints a warning message.
     *
     * @version 1.8
     */
    public synchronized void notifyJVMDeoptWarmUpMethods() {
        if (hasNotified && checkIfCompilationIsComplete()) {
            notifyJVMDeoptWarmUpMethods0();
        }
    }

    /**
     * Check if the last compilation submitted by JWarmUp is complete.
     * <p>
     * call this method after <code>notifyApplicationStartUpIsDone</code>
     *
     * @return true if the last compilation task is complete.
     *
     * @version 1.8
     */
    public synchronized boolean checkIfCompilationIsComplete() {
        if (!hasNotified) {
          throw new IllegalStateException("Must call checkIfCompilationIsComplete() after notifyApplicationStartUpIsDone()");
        } else {
          return checkIfCompilationIsComplete0();
        }
    }


    /**
     * dummy function used internal, DO NOT call this
     */
    private void dummy() {
        throw new UnsupportedOperationException("dummy function");
    }

    private native void notifyApplicationStartUpIsDone0();

    private native boolean checkIfCompilationIsComplete0();

    private native void notifyJVMDeoptWarmUpMethods0();
}