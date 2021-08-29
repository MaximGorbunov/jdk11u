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

import java.lang.reflect.Method;
import java.util.*;
import java.io.File;
import jdk.jwarmup.JWarmUp;
import static jdk.test.lib.Asserts.*;
import jdk.test.lib.process.*;
import jdk.test.lib.Platform;

/*
 * @test TestVirtualCallWithNullReceiver
 * @library /test/lib
 * @build TestVirtualCallWithNullReceiver
 * @run main/othervm TestVirtualCallWithNullReceiver
 * @summary test to verify jwarmup method doesn't get de-optimized many times if virtual call receiver is null
 */
public class TestVirtualCallWithNullReceiver {

    public static JWarmUp jWarmUp = JWarmUp.getJWarmUp();
    public static void main(String[] args) throws Exception {
        if (!Platform.isLinux()) {
            System.out.println("Passed");
            return;
        }
        ProcessBuilder pb = null;
        OutputAnalyzer output = null;
        File logfile = null;
        String classPath = System.getProperty("test.class.path");
        try {
            output = ProcessTools.executeProcess("pwd");
            System.out.println(output.getOutput());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        pb = ProcessTools.createJavaProcessBuilder("-XX:-TieredCompilation",
                "-Xbootclasspath/a:.",
                "-XX:+CompilationWarmUpRecording",
                "-XX:-ClassUnloading",
                "-XX:-Inline",
                "-XX:+UseConcMarkSweepGC",
                "-XX:-CMSClassUnloadingEnabled",
                "-XX:-UseSharedSpaces",
                "-XX:CompilationWarmUpLogfile=./jitwarmup.log",
                "-XX:+PrintCompilationWarmUpDetail",
                "-XX:CompilationWarmUpRecordTime=10",
                "-XX:+PrintCompilation",
                "-XX:+UnlockDiagnosticVMOptions",
                "-cp", classPath,
                InnerA.class.getName(), "recording");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("foo");
        output.shouldHaveExitValue(0);
        System.out.println(output.getOutput());

        pb = ProcessTools.createJavaProcessBuilder("-XX:-TieredCompilation",
                "-Xbootclasspath/a:.",
                "-XX:+CompilationWarmUp",
                "-XX:-Inline",
                "-XX:-UseSharedSpaces",
                // "--add-modules java.base,jdk.jwarmup",
                // "--add-exports java.base/jdk.jwarmup=ALL-UNNAMED",
                "-XX:CompilationWarmUpLogfile=./jitwarmup.log",
                "-XX:+PrintCompilationWarmUpDetail",
                "-XX:+PrintCompilation",
                "-XX:+UnlockDiagnosticVMOptions", //"-XX:+WhiteBoxAPI",
                "-XX:+CompilationWarmUpResolveClassEagerly",
                "-cp", classPath,
                InnerA.class.getName(), "startup");
        output = new OutputAnalyzer(pb.start());
        System.out.println(output.getOutput());
        output.shouldHaveExitValue(0);
        assertTrue(getDeoptimizationCount(output.getOutput()) == 1);

        /* This test is not stable
        pb = ProcessTools.createJavaProcessBuilder("-XX:-TieredCompilation",
                "-Xbootclasspath/a:.",
                "-XX:+CompilationWarmUp",
                "-XX:-Inline",
                "-XX:-UseSharedSpaces",
                "-XX:CompilationWarmUpLogfile=./jitwarmup.log",
                "-XX:+PrintCompilationWarmUpDetail",
                "-XX:+PrintCompilation",
                "-XX:+UnlockDiagnosticVMOptions", //"-XX:+WhiteBoxAPI",
                "-XX:+CompilationWarmUpResolveClassEagerly",
                "-cp", classPath,
                InnerA.class.getName(), "startup", "doNotCheck");
        output = new OutputAnalyzer(pb.start());
        System.out.println(output.getOutput());
        output.shouldHaveExitValue(0);
        assertTrue(getDeoptimizationCount(output.getOutput()) > 1);
        logfile = new File("./jitwarmup.log");
        if (logfile.exists()) {
            try {
                logfile.delete();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        */
    }

    public static int getDeoptimizationCount(String output) {
        String[] lines = output.split("\n");
        int count = 0;
        for (String line : lines) {
            if (line.contains("::foo") && line.contains("made not entrant")) {
                count++;
            }
        }
        return count;
    }

    public static class InnerA {
        static {
            System.out.println("InnerA initialize");
        }

        @Override
        public int hashCode() {
            if (ls.size() > 1) {
                System.out.println("avoid optimization");
            }
            return 1;
        }

        public static List<String> ls = new ArrayList<String>();

        public static void foo(InnerA a) {
            a.hashCode();
            if (ls.size() > 1) {
                System.out.println("avoid optimization");
            }
        }

        public static void main(String[] args) throws Exception {
            if ("recording".equals(args[0])) {
                System.out.println("begin recording!");
                // initialize InnerA
                InnerA a = new InnerA();
                for (int i = 0; i < 20000; i++) {
                    foo(a);
                }
                Thread.sleep(1000);
                foo(a);
                Thread.sleep(10000);
                System.out.println("done!");
            } else {
                System.out.println("begin startup!");
                jWarmUp.notifyApplicationStartUpIsDone();
                // doNotCheck not in args
                if (args.length < 2) {
                    while (!jWarmUp.checkIfCompilationIsComplete()) {
                        Thread.sleep(1000);
                    }
                }
                for (int i = 0; i < 100000; i++) {
                    try {
                          InnerA a = new InnerA();
                          foo(a);
                          foo(null);
                          if ( i % 10000 == 1) {
                              System.gc();
                          }
                    } catch (Exception e) {
                        // do nothing
                    }
                }
                System.out.println("done!");
            }
        }
    }
}