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
import io.ballerina.shell.cli.help.HelpProvider;
import io.ballerina.shell.cli.help.RemoteBbeHelpProvider;
import io.ballerina.shell.exceptions.BallerinaShellException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * REPL shell terminal executor. Launches the terminal.
 * Independent of third party libraries.
 */
public class BallerinaShell {
    private static final String HELP_FILE = "command.help.txt";
    private static final String HEADER_FILE = "command.header.txt";
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String REPL_PROMPT = "=$ ";

    private static final String COMMAND_PREFIX = "/";
    private static final String HELP_COMMAND = "/help";
    private static final String EXIT_COMMAND = "/exit";
    private static final String TOGGLE_DEBUG = "/debug";
    private static final String RESET_COMMAND = "/reset";
    private static final String STATE_COMMAND = "/state";
    private static final String IMPORTS_COMMAND = "/imports";
    private static final String MODULE_DCLNS_COMMAND = "/dclns";
    private static final String VARIABLES_COMMAND = "/vars";

    protected final Configuration configuration;
    protected final TerminalAdapter terminal;
    protected final Evaluator evaluator;
    protected final Map<String, Consumer<String[]>> handlers;
    private final HelpProvider helpProvider;
    protected boolean continueLoop;

    public BallerinaShell(Configuration configuration, TerminalAdapter terminal) {
        this.configuration = configuration;
        this.terminal = terminal;
        this.continueLoop = true;
        this.evaluator = configuration.getEvaluator();
        this.helpProvider = new RemoteBbeHelpProvider();
        this.handlers = availableCommands();
    }

    /**
     * Runs the terminal application using the given config.
     */
    public void run() {
        String leftPrompt = terminal.color(REPL_PROMPT, TerminalAdapter.GREEN);
        terminal.println(readFile(HEADER_FILE));

        // Initialize. This must not fail.
        // If this fails, an error would be directly thrown.
        Instant start = Instant.now();
        try {
            evaluator.initialize();
        } catch (BallerinaShellException e) {
            throw new RuntimeException("Shell initialization failed.", e);
        }
        Instant end = Instant.now();

        while (continueLoop) {
            Duration previousDuration = Duration.between(start, end);
            String rightPrompt = String.format("took %s ms", previousDuration.toMillis());
            String source = terminal.readLine(leftPrompt, rightPrompt).trim();
            start = Instant.now();

            try {
                // Ignore blank lines
                if (source.isBlank()) {
                    continue;
                }
                // Check if starts with /
                if (source.startsWith(COMMAND_PREFIX)) {
                    String[] args = source.split(" ");
                    if (this.handlers.containsKey(args[0])) {
                        this.handlers.get(args[0]).accept(args);
                        continue;
                    }
                }
                // Evaluate line
                String result = evaluator.evaluate(source);
                if (result != null) {
                    terminal.result(result);
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
     * Reads the file content from the resources.
     *
     * @param path Path of the file to read.
     * @return Read text.
     */
    private String readFile(String path) {
        ClassLoader classLoader = BallerinaShell.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        Objects.requireNonNull(inputStream, "File does not exist: " + path);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Output a diagnostic to the terminal.
     *
     * @param diagnostic Diagnostic to output.
     */
    private void outputDiagnostic(Diagnostic diagnostic) {
        if (diagnostic.getKind() == DiagnosticKind.DEBUG && !configuration.isDebug()) {
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
    private void outputException(Exception e) {
        if (configuration.isDebug()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            terminal.fatalError(stringWriter.toString());
        }
    }

    // Commands  ======================================================

    /**
     * Attaches commands to the handler which handles internal command.
     *
     * @return Command attached handler map.
     */
    protected Map<String, Consumer<String[]>> availableCommands() {
        Map<String, Consumer<String[]>> handlers = new HashMap<>();
        handlers.put(RESET_COMMAND, this::handleReset);
        handlers.put(HELP_COMMAND, this::handleHelp);
        handlers.put(TOGGLE_DEBUG, this::handleToggleDebug);
        handlers.put(STATE_COMMAND, this::handleDumpState);
        handlers.put(IMPORTS_COMMAND, this::handleDumpImports);
        handlers.put(VARIABLES_COMMAND, this::handleDumpVars);
        handlers.put(MODULE_DCLNS_COMMAND, this::handleDumpDclns);
        handlers.put(EXIT_COMMAND, this::handleExit);
        return handlers;
    }

    protected void handleReset(String... args) {
        this.evaluator.reset();
        terminal.info("REPL state was reset.");
    }

    protected void handleHelp(String... args) {
        StringBuilder content = new StringBuilder();
        if (args.length <= 1) {
            content.append(readFile(HELP_FILE));
        } else {
            helpProvider.getTopic(args[1], content);
        }
        terminal.info(content.toString());
    }

    protected void handleToggleDebug(String... args) {
        this.configuration.toggleDebug();
        terminal.info("Toggled debug mode. Debug mode: "
                + this.configuration.isDebug);
    }

    protected void handleDumpState(String... args) {
        this.terminal.info(evaluator.toString());
    }

    protected void handleDumpImports(String... args) {
        this.terminal.info(evaluator.availableImports());
    }

    protected void handleDumpVars(String... args) {
        this.terminal.info(evaluator.availableVariables());
    }

    protected void handleDumpDclns(String... args) {
        this.terminal.info(evaluator.availableModuleDeclarations());
    }

    protected void handleExit(String... args) {
        this.continueLoop = false;
        terminal.info("Bye!!");
    }
}
