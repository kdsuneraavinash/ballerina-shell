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

package org.ballerina.repl;

import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.jar.JarExecutor;
import io.ballerina.shell.executor.reeval.ReEvalExecutor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.jline.terminal.Terminal;

import java.util.Objects;

/**
 * Object to store configurations related to the REPL.
 */
public class ReplConfiguration {
    private static final String REEVAL_EXECUTOR = "reeval";
    private static final String ASM_EXECUTOR = "asm";

    private final Terminal terminal;
    private final String executorName;
    private boolean isDebugMode;

    public ReplConfiguration(CommandLine cmd, Terminal terminal) throws HelpException {
        Objects.requireNonNull(cmd, "Command line arguments were not received.");
        Objects.requireNonNull(terminal, "Terminal objects were not received.");

        if (ApplicationOption.HELP.hasOptionSet(cmd)) {
            throw new HelpException();
        }
        isDebugMode = ApplicationOption.DEBUG.hasOptionSet(cmd);
        executorName = ApplicationOption.EXECUTOR.getOptionValue(cmd, REEVAL_EXECUTOR);
        this.terminal = terminal;
        setDiagnosticOutputMode();
    }

    /**
     * Generate the CLI options.
     * These options will be used by the CLI parser to
     * get the necessary configurations.
     *
     * @return Generated CLI options.
     */
    public static Options getConfigurationOptions() {
        Options options = new Options();
        for (ApplicationOption op : ApplicationOption.values()) {
            Option option = op.toOption();
            options.addOption(option);
        }
        return options;
    }

    /**
     * Changes the debug mode on/off.
     */
    public void toggleDiagnosticOutputMode() {
        isDebugMode = !isDebugMode;
        setDiagnosticOutputMode();
    }

    /**
     * Sets the diagnostic mode depending on the debug mode.
     */
    private void setDiagnosticOutputMode() {
        if (isDebugMode) {
            PrinterProvider.setWriter(new ReplPrinterService(terminal, false));
            PrinterProvider.debug("Diagnostic output mode set to ON.");
        } else {
            PrinterProvider.debug("Diagnostic output mode set to OFF.");
            PrinterProvider.setWriter(new ReplPrinterService(terminal, true));
        }
    }

    /**
     * Returns an executor depending on the config.
     *
     * @return a new Executor object.
     */
    public Executor<?, ?, ?> getExecutor() {
        if (executorName.equalsIgnoreCase(REEVAL_EXECUTOR)) {
            return new ReEvalExecutor();
        } else if (executorName.equalsIgnoreCase(ASM_EXECUTOR)) {
            return new JarExecutor();
        }
        throw new RuntimeException("Unknown executor name: " + executorName);
    }

    /**
     * Prints stack tract depending on the debug mode.
     *
     * @param e Exception to print stack trace.
     */
    public void printStackTrace(Exception e) {
        if (isDebugMode) {
            e.printStackTrace(terminal.writer());
        }
    }
}
