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
package io.ballerina.shell.executor;

import io.ballerina.shell.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.executor.process.ProcessInvoker;
import io.ballerina.shell.executor.process.ShellProcessInvoker;
import io.ballerina.shell.executor.wrapper.Wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * An executor that will delegate the process to the external ballerina executable command.
 * Ballerina must be installed and on path to use this executor.
 * This would directly call the ballerina command.
 */
public class ProcessExecutor extends StatelessExecutor {
    private static final String BALLERINA_COMMAND = "ballerina run %s";
    private static final String TEMP_FILE = "main.bal";
    private final ProcessInvoker processInvoker;

    public ProcessExecutor(Wrapper wrapper) {
        super(wrapper);
        String command = String.format(BALLERINA_COMMAND, TEMP_FILE);
        processInvoker = new ShellProcessInvoker(command);
        ShellDiagnosticProvider.sendMessage("Using process executor with shell process invoker.");
        ShellDiagnosticProvider.sendMessage("Shell command invocation used: " + command);
    }

    @Override
    protected ExecutorResult evaluateSourceCode(String sourceCode) {
        File file = new File(TEMP_FILE);
        try (FileWriter fileWriter = new FileWriter(file, Charset.defaultCharset())) {
            fileWriter.write(sourceCode);
        } catch (IOException e) {
            throw new RuntimeException("Target file write failed.", e);
        }

        try {
            // Execute and return correct output.
            processInvoker.execute();
            List<String> standardStrings = processInvoker.isErrorExit()
                    ? processInvoker.getStandardError()
                    : processInvoker.getStandardOutput();

            String output = String.join("\n", standardStrings);
            return new ExecutorResult(processInvoker.isErrorExit(), output);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
