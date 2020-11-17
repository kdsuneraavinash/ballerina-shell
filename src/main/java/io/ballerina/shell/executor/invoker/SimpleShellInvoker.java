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

import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.postprocessor.Postprocessor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * External process information and invoker.
 * Invokes a ballerina run command to compile and evaluate a file.
 * This will wait until command is finished and output the stdout/stderr.
 */
@SuppressWarnings("unused")
public class SimpleShellInvoker extends ShellInvoker {
    private static final String BALLERINA_COMMAND = "ballerina run %s";
    protected final String command;

    public SimpleShellInvoker(String file) {
        String command = String.format(BALLERINA_COMMAND, file);
        PrinterProvider.debug("Shell command invocation used: " + command);
        this.command = command;
    }

    @Override
    public boolean execute(Postprocessor postprocessor) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.waitFor();

        Charset defaultCharset = Charset.defaultCharset();
        try (Scanner stdOutScanner = new Scanner(process.getInputStream(), defaultCharset);
             Scanner stdErrScanner = new Scanner(process.getErrorStream(), defaultCharset)) {
            while (stdOutScanner.hasNextLine()) {
                postprocessor.onProgramOutput(stdOutScanner.nextLine());
            }
            while (stdErrScanner.hasNextLine()) {
                postprocessor.onCompilerOutput(stdErrScanner.nextLine());
            }
        }

        int exitCode = process.exitValue();
        PrinterProvider.debug(
                "Execution finished with exit code " + exitCode);
        return exitCode == 0;
    }
}
