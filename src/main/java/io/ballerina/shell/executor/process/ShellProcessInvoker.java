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
package io.ballerina.shell.executor.process;

import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * External process information and invoker.
 * Invokes a shell command given and provides APIs to retrieve stdout/stderr/exit code.
 */
public class ShellProcessInvoker implements ProcessInvoker {
    private final String command;
    private final List<String> standardOutput;
    private final List<String> standardError;
    private int exitCode;

    public ShellProcessInvoker(String command) {
        this.command = command;
        this.standardOutput = new ArrayList<>();
        this.standardError = new ArrayList<>();
        this.exitCode = 0;
    }

    @Override
    public void execute() throws IOException, InterruptedException {
        this.standardOutput.clear();
        this.standardError.clear();

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.waitFor();
        this.exitCode = process.exitValue();

        Charset defaultCharset = Charset.defaultCharset();
        try (
                InputStreamReader stdOutReader = new InputStreamReader(process.getInputStream(), defaultCharset);
                InputStreamReader stdErrReader = new InputStreamReader(process.getErrorStream(), defaultCharset);
                BufferedReader stdOutBuff = new BufferedReader(stdOutReader);
                BufferedReader stdErrBuff = new BufferedReader(stdErrReader)
        ) {
            String line;
            while ((line = stdOutBuff.readLine()) != null) {
                this.standardOutput.add(line);
            }
            while ((line = stdErrBuff.readLine()) != null) {
                this.standardError.add(line);
            }
            if (isErrorExit()) {
                ShellDiagnosticProvider.sendMessage(
                        "Execution failed with exit code %s.", String.valueOf(exitCode));
            }
        }
    }

    @Override
    public boolean isErrorExit() {
        return exitCode != 0;
    }

    @Override
    public List<String> getStandardError() {
        return standardError;
    }

    @Override
    public List<String> getStandardOutput() {
        return standardOutput;
    }
}
