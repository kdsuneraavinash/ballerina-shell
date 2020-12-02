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

package io.ballerina.shell.cli;

import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.DiagnosticKind;
import io.ballerina.shell.Evaluator;
import io.ballerina.shell.cli.handlers.CommandHandler;
import io.ballerina.shell.cli.handlers.ExitCommand;
import io.ballerina.shell.cli.handlers.HelpCommand;
import io.ballerina.shell.cli.handlers.ResetStateCommand;
import io.ballerina.shell.cli.handlers.StringInfoCommand;
import io.ballerina.shell.cli.handlers.ToggleDebugCommand;
import io.ballerina.shell.cli.utils.FileUtils;
import io.ballerina.shell.exceptions.BallerinaShellException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_DCLNS;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_DEBUG;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_EXIT;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_HELP;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_IMPORTS;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_RESET;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_STATE;
import static io.ballerina.shell.cli.PropertiesLoader.COMMAND_VARS;
import static io.ballerina.shell.cli.PropertiesLoader.HEADER_FILE;
import static io.ballerina.shell.cli.PropertiesLoader.REPL_PROMPT;

/**
 * REPL shell terminal executor. Launches the terminal.
 * Independent of third party libraries.
 */
public class BallerinaShell {
    private static final int INIT_WARNING_THRESH_S = 2;

    protected final Configuration configuration;
    protected final TerminalAdapter terminal;
    protected final Evaluator evaluator;
    protected final CommandHandler commandHandler;
    protected boolean isRunning;

    public BallerinaShell(Configuration configuration, TerminalAdapter terminal) {
        this.configuration = configuration;
        this.terminal = terminal;
        this.isRunning = true;
        this.evaluator = configuration.getEvaluator();
        this.commandHandler = createCommandHandler();
    }

    /**
     * Runs the terminal application using the given config.
     */
    public void run() {
        String leftPrompt = terminal.color(PropertiesLoader.getProperty(REPL_PROMPT),
                TerminalAdapter.GREEN);
        terminal.println(FileUtils.readResource(PropertiesLoader.getProperty(HEADER_FILE)));

        Instant start = Instant.now();
        // Initialize. This must not fail.
        // If this fails, an error would be directly thrown.
        try {
            evaluator.initialize();
        } catch (BallerinaShellException e) {
            throw new RuntimeException("Shell initialization failed.", e);
        }
        Instant end = Instant.now();
        // Output a warning if initialization took too long.
        if (Duration.between(start, end).getSeconds() > INIT_WARNING_THRESH_S) {
            terminal.warn("Compiler initialization took longer than expected.");
        }

        while (isRunning) {
            Duration previousDuration = Duration.between(start, end);
            String rightPrompt = String.format("took %s ms", previousDuration.toMillis());
            rightPrompt = terminal.color(rightPrompt, TerminalAdapter.BRIGHT);

            String source = terminal.readLine(leftPrompt, rightPrompt).trim();

            start = Instant.now();
            try {
                if (!commandHandler.handle(source)) {
                    String result = evaluator.evaluate(source);
                    if (result != null) {
                        terminal.result(result);
                    }
                }
            } catch (Exception e) {
                if (!evaluator.hasErrors()) {
                    terminal.fatalError("Something went wrong: " + e.getMessage());
                }
                outputException(e);
            } finally {
                end = Instant.now();
                evaluator.diagnostics().forEach(this::outputDiagnostic);
                evaluator.resetDiagnostics();
                terminal.println("");
            }
        }
    }

    /**
     * Output a diagnostic to the terminal.
     *
     * @param diagnostic Diagnostic to output.
     */
    protected void outputDiagnostic(Diagnostic diagnostic) {
        if (diagnostic.getKind() == DiagnosticKind.DEBUG &&
                !configuration.isDebug()) {
            return;
        }

        if (diagnostic.getKind() == DiagnosticKind.ERROR) {
            terminal.error(diagnostic.toString());
        } else if (diagnostic.getKind() == DiagnosticKind.WARN) {
            terminal.warn(diagnostic.toString());
        } else {
            terminal.debug(diagnostic.toString());
        }
    }

    /**
     * Outputs an exception to the terminal.
     *
     * @param e Exception to output.
     */
    protected void outputException(Exception e) {
        if (configuration.isDebug()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            terminal.fatalError(stringWriter.toString());
        }
    }

    /**
     * Attaches commands to the handler which handles internal command.
     *
     * @return Command attached handler.
     */
    protected CommandHandler createCommandHandler() {
        CommandHandler commandHandler = new CommandHandler();
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_EXIT), new ExitCommand(this));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_HELP), new HelpCommand(this));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_RESET), new ResetStateCommand(this));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_DEBUG), new ToggleDebugCommand(this));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_STATE),
                new StringInfoCommand(this, evaluator::toString));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_VARS),
                new StringInfoCommand(this, evaluator::availableVariables));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_IMPORTS),
                new StringInfoCommand(this, evaluator::availableImports));
        commandHandler.attach(PropertiesLoader.getProperty(COMMAND_DCLNS),
                new StringInfoCommand(this, evaluator::availableModuleDeclarations));
        return commandHandler;
    }

    public void outputInfo(String text) {
        this.terminal.info(text);
    }

    public void outputError(String text) {
        this.terminal.error(text);
    }

    public void toggleDebug() {
        this.configuration.toggleDebug();
    }

    public void reset() {
        this.evaluator.reset();
    }

    public void exit() {
        this.isRunning = false;
    }
}
