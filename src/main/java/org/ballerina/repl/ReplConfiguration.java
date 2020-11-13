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

import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.SourceGenExecutor;
import io.ballerina.shell.executor.TemplateExecutor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ballerina.repl.exceptions.ReplCmdHelpException;
import org.jline.terminal.Terminal;

import java.util.Objects;

/**
 * Object to store configurations related to the REPL.
 */
public class ReplConfiguration {
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private final Terminal terminal;
    private final String executorName;
    private boolean isDebugMode;

    public ReplConfiguration(CommandLine cmd, Terminal terminal) throws ReplCmdHelpException {
        Objects.requireNonNull(cmd, "Command line arguments were not received.");
        Objects.requireNonNull(terminal, "Terminal objects were not received.");

        if (cmd.hasOption(ReplCommandOption.HELP.option())) {
            throw new ReplCmdHelpException();
        }
        isDebugMode = cmd.hasOption(ReplCommandOption.DEBUG.option());
        executorName = cmd.getOptionValue(ReplCommandOption.EXECUTOR.option(), "gen");
        this.terminal = terminal;
        setDiagnosticOutputMode();
    }

    /**
     * Generate the CLI options.
     *
     * @return Generated CLI options.
     */
    public static Options getCommandLineOptions() {
        Options options = new Options();
        for (ReplCommandOption op : ReplCommandOption.values()) {
            Option option = new Option(op.option(), op.name, op.hasArg, op.description);
            option.setRequired(false);
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
            ShellDiagnosticProvider.getInstance().setWriter(new ReplDiagnosticWriter(terminal));
            ShellDiagnosticProvider.sendMessage("Diagnostic output mode set to ON.");
        } else {
            ShellDiagnosticProvider.sendMessage("Diagnostic output mode set to OFF.");
            ShellDiagnosticProvider.getInstance().setWriter(null);
        }
    }

    /**
     * Returns an executor depending on the config.
     *
     * @return a new Executor object.
     */
    public Executor getExecutor() {
        if (executorName.equalsIgnoreCase("gen")) {
            return new SourceGenExecutor();
        } else if (executorName.equalsIgnoreCase("reeval")) {
            return new TemplateExecutor();
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
