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

package io.ballerina.shell.executor.dynamic.invoker;

import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.executor.dynamic.DynamicState;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.utils.InterceptedPrintStream;
import io.ballerina.shell.utils.NoExitVmSecurityManager;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Scanner;

/**
 * Invoker that will use ballerina shell command to
 * build the project and then manipulate the jar file to
 * load/state states so that execution can happen in a interpreted manner.
 */
public class DynamicShellInvoker extends DynamicInvoker {
    private static final String BALLERINA_BUILD_COMMAND = "ballerina build %s";
    private static final String BUILT_JAR = "main.jar";
    private static final String RUN_CLASS_REF = "$_init";
    private static final String MAIN_METHOD_REF = "main";
    private static final String INTERNAL_INDICATOR = "$";
    private static final String FILE_STD_ERR = "stdout.txt";

    private final NoExitVmSecurityManager securityManager;
    private final String buildCommand;

    public DynamicShellInvoker(String file) {
        this.buildCommand = String.format(BALLERINA_BUILD_COMMAND, file);
        DebugProvider.sendMessage("Shell command invocation used: " + buildCommand);
        securityManager = new NoExitVmSecurityManager();
    }

    @Override
    public boolean execute(DynamicState state, Postprocessor postprocessor)
            throws IOException, InterruptedException, ClassNotFoundException {
        // Compile
        // TODO: Delete ballerina internal log. This is created for every execution.
        boolean isCompilationSuccess = buildJar(postprocessor);
        if (!isCompilationSuccess) {
            return false;
        }

        // Get runner ref
        URL[] jarFileUrl = {new File(BUILT_JAR).toURI().toURL()};
        URLClassLoader child = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public URLClassLoader run() {
                return new URLClassLoader(jarFileUrl, this.getClass().getClassLoader());
            }
        });

        Class<?> runnerRef = Class.forName(RUN_CLASS_REF, false, child);
        Objects.requireNonNull(runnerRef, "Runner file loading failed.");

        // Run
        loadState(runnerRef, state);
        runJar(runnerRef, postprocessor);
        saveState(runnerRef, state);

        return true;
    }

    /**
     * Compiles and builds the JAR file for the ballerina file.
     * Will also output compilation errors to STDOUT.
     *
     * @param postprocessor Postprocessor to use to output.
     * @return Whether the building succeeded.
     * @throws InterruptedException If waiting for the compilation failed.
     * @throws IOException          If command execution failed.
     */
    private boolean buildJar(Postprocessor postprocessor) throws InterruptedException, IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(buildCommand);
        process.waitFor();
        try (Scanner scanner = new Scanner(process.getErrorStream(), Charset.defaultCharset())) {
            while (scanner.hasNextLine()) {
                postprocessor.onCompilerOutput(scanner.nextLine());
            }
        }
        int compilerExitCode = process.exitValue();
        if (compilerExitCode != 0) {
            DebugProvider.sendMessage("Compiler error. Exit code %s.",
                    String.valueOf(compilerExitCode));
            return false;
        }
        return true;
    }

    /**
     * Runs the JAR that is built.
     * Will redirect all the output in STDERR/STDOUT to a file.
     * STDOUT will also be redirected to the postprocessor.
     *
     * @param runnerRef     Class runner which contains the main method.
     * @param postprocessor Postprocessor to use to output.
     * @throws FileNotFoundException If alternate STDERR/STDOUT file open failed.
     */
    private void runJar(Class<?> runnerRef, Postprocessor postprocessor) throws IOException {
        // Alternate STDERR (STDOUT and STDERR will be redirected here)
        PrintStream altStdErr = new PrintStream(FILE_STD_ERR, Charset.defaultCharset());
        PrintStream altStdOut = new InterceptedPrintStream(altStdErr, postprocessor::onProgramOutput);

        // Setup the context
        PrintStream originalStdOut = System.out; // Save STDOUT
        PrintStream originalStdErr = System.err; // Save STDERR
        System.setOut(altStdOut); // Change STDOUT
        System.setErr(altStdErr); // Change STDERR
        System.setSecurityManager(securityManager); // Add security manager

        // Run the JAR
        try {
            Method method = runnerRef.getDeclaredMethod(MAIN_METHOD_REF, String[].class);
            method.invoke(null, (Object) new String[]{});
        } catch (InvocationTargetException ignored) {
        } catch (Exception e) {
            throw new ExecutorException(e);
        } finally {
            // Restore context
            System.setSecurityManager(null); // Remove security manager
            System.setOut(originalStdErr); // Restore STDERR
            System.setOut(originalStdOut); // Restore STDOUT
        }
    }

    /**
     * Load the state to the Runner class for the state.
     *
     * @param runnerRef Class reference of the main class.
     * @param state     Previously preserved state.
     */
    private void loadState(Class<?> runnerRef, DynamicState state) {
        try {
            for (Field field : runnerRef.getDeclaredFields()) {
                String fieldName = field.getName();
                if (!fieldName.startsWith(INTERNAL_INDICATOR)) {
                    if (state.containsVariableState(fieldName)) {
                        field.set(null, state.getVariableState(fieldName));
                        DebugProvider.sendMessage(fieldName + " loaded as " + state.getVariableState(fieldName));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new ExecutorException(e);
        }
    }

    /**
     * Save the state of Runner to the state.
     *
     * @param runnerRef Class reference of the main class
     * @param state     State to write data into.
     */
    private void saveState(Class<?> runnerRef, DynamicState state) {
        try {
            for (Field field : runnerRef.getDeclaredFields()) {
                String fieldName = field.getName();
                if (!fieldName.startsWith(INTERNAL_INDICATOR)) {
                    state.setVariableState(fieldName, field.get(null));
                    DebugProvider.sendMessage(fieldName + " saved as " + state.getVariableState(fieldName));
                }
            }
        } catch (IllegalAccessException e) {
            throw new ExecutorException(e);
        }
    }
}
