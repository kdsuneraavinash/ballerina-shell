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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * External process information and invoker.
 * Invokes a shell command given.
 */
public class ShellProcessInvoker implements ProcessInvoker {
    protected final String command;

    public ShellProcessInvoker(String command) {
        this.command = command;
    }

    @Override
    public boolean execute(Postprocessor postprocessor) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        process.waitFor();

        Charset defaultCharset = Charset.defaultCharset();
        try (
                InputStreamReader stdOutReader = new InputStreamReader(process.getInputStream(), defaultCharset);
                InputStreamReader stdErrReader = new InputStreamReader(process.getErrorStream(), defaultCharset);
                BufferedReader stdOutBuff = new BufferedReader(stdOutReader);
                BufferedReader stdErrBuff = new BufferedReader(stdErrReader)
        ) {
            String line;
            while ((line = stdOutBuff.readLine()) != null) {
                postprocessor.onProgramOutput(line);
            }
            while ((line = stdErrBuff.readLine()) != null) {
                postprocessor.onCompilerOutput(line);
            }
        }

        int exitCode = process.exitValue();
        ShellDiagnosticProvider.sendMessage(
                "Execution finished with exit code %s.", String.valueOf(exitCode));
        return exitCode == 0;
    }
}
