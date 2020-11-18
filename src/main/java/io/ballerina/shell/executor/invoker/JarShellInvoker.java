/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.shell.executor.invoker;

import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.utils.InterceptedPrintStream;
import io.ballerina.shell.utils.NoExitVmSecurityManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Scanner;

/**
 * Invokes the Jar file directly after building it.
 */
public class JarShellInvoker extends ShellInvoker {
    private static final String BALLERINA_BUILD_COMMAND = "ballerina build %s";
    private static final String ALT_STDOUT = "stdout.txt";
    private final String command;
    private final String jarFile;

    public JarShellInvoker(String balFile, String jarFile) {
        this.command = String.format(BALLERINA_BUILD_COMMAND, balFile);
        this.jarFile = jarFile;
    }

    @Override
    public boolean execute(Postprocessor postprocessor) throws IOException, InterruptedException, ExecutorException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.waitFor();

        // Output compiler output
        try (Scanner err = new Scanner(process.getErrorStream(), Charset.defaultCharset())) {
            while (err.hasNextLine()) {
                postprocessor.onCompilerOutput(err.nextLine());
            }
        }

        if (process.exitValue() != 0) {
            return false;
        }

        // Setup context
        try (FileOutputStream fos = new FileOutputStream(ALT_STDOUT)) {
            PrintStream intercept = new InterceptedPrintStream(fos, postprocessor::onProgramOutput);
            PrintStream originalStdOut = System.out; // Take backup STDOUT
            PrintStream originalStdErr = System.err; // Take backup STDERR
            System.setOut(intercept);
            System.setErr(intercept);
            System.setSecurityManager(new NoExitVmSecurityManager());

            try {
                URL[] jarPath = {Paths.get(jarFile).toUri().toURL()};
                URLClassLoader child = AccessController.doPrivileged(new PrivilegedAction<>() {
                    @Override
                    public URLClassLoader run() {
                        return new URLClassLoader(jarPath, this.getClass().getClassLoader());
                    }
                });
                Class<?> classToLoad = Class.forName("$_init", true, child);
                Method method = classToLoad.getDeclaredMethod("main", String[].class);
                method.invoke(null, new Object[]{new String[]{}});
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | ClassNotFoundException e) {
                throw new ExecutorException("Something went wrong: " + e.getMessage());
            } catch (InvocationTargetException e) {
                // ignore
            } finally {
                // Restore context
                System.setSecurityManager(null);
                System.setOut(originalStdOut); // Restore STDOUT
                System.setErr(originalStdErr); // Restore STDERR
            }
        }

        return true;
    }
}
