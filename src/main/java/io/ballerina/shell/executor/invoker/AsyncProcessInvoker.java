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

import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Invokes the process and asynchronously listens to the STDOUT/STDERR streams.
 * So the program updates would not be blocked until process is finished.
 */
public class AsyncProcessInvoker extends ShellProcessInvoker {
    public AsyncProcessInvoker(String command) {
        super(command);
    }

    @Override
    public boolean execute(Postprocessor postprocessor) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);

        // Threads to manage STDOUT/STDERR
        Thread stdOutThread = inputStreamThread("STDOUT", process.getInputStream(), postprocessor::onProgramOutput);
        Thread stdErrThread = inputStreamThread("STDERR", process.getErrorStream(), postprocessor::onCompilerOutput);
        // Start the threads
        stdOutThread.start();
        stdErrThread.start();

        // Wait for process to finish
        process.waitFor();
        int exitCode = process.exitValue();

        // Wait for threads to finish
        stdOutThread.interrupt();
        stdErrThread.interrupt();
        stdOutThread.join();
        stdErrThread.join();


        ShellDiagnosticProvider.sendMessage("Execution finished with exit code %s.", String.valueOf(exitCode));
        assert !stdOutThread.isAlive() && !stdErrThread.isAlive();
        return exitCode == 0;
    }

    private Thread inputStreamThread(String name, InputStream inputStream, Consumer<String> stringConsumer) {
        return new Thread(() -> {
            try (Scanner scanner = new Scanner(inputStream, Charset.defaultCharset())) {
                while (scanner.hasNextLine()) {
                    stringConsumer.accept(scanner.nextLine());
                }
            }
            ShellDiagnosticProvider.sendMessage("Thread %s exiting.", String.valueOf(name));
        }, name);
    }
}
