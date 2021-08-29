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

import java.io.File;
import jdk.test.lib.process.*;
import jdk.test.lib.Platform;

/*
 * @test
 * @summary Test JitWarmUp model required flag
 *
 * @library /test/lib
 * @run main TestFlagAssertion
 */
public class TestFlagAssertion {
    public static void main(String[] args) throws Exception {
        if (!Platform.isLinux()) {
            System.out.println("Passed");
            return;
        }
        ProcessBuilder pb;
        OutputAnalyzer output;

        pb = ProcessTools.createJavaProcessBuilder("-XX:+CompilationWarmUpRecording",
                "-XX:+TieredCompilation",
                "-XX:-ProfileInterpreter",
                "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("[JitWarmUp] ERROR: flag ProfileInterpreter must be on");
        output.shouldContain("[JitWarmUp] ERROR: init error");
        System.out.println(output.getOutput());

        pb = ProcessTools.createJavaProcessBuilder("-XX:+CompilationWarmUpRecording",
                "-XX:-TieredCompilation",
                "-XX:+ClassUnloading",
                "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("[JitWarmUp] ERROR: flag ClassUnloading must be off");
        output.shouldContain("[JitWarmUp] ERROR: init error");
        System.out.println(output.getOutput());
    }
}