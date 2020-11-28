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
import io.ballerina.shell.exceptions.BallerinaShellException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
    private static final String REPL_VERSION = "0.0.1";
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String REPL_PROMPT = "=$ ";
    private static final String REPL_EXIT_MESSAGE = "Bye!!";

    private static final String HELP_COMMAND = "/help";
    private static final String EXIT_COMMAND = "/exit";
    private static final String TOGGLE_DEBUG = "/debug";
    private static final String RESET_COMMAND = "/reset";
    private static final String STATE_COMMAND = "/state";
    private static final String IMPORTS_COMMAND = "/imports";
    private static final String MODULE_DCLNS_COMMAND = "/dclns";
    private static final String VARIABLES_COMMAND = "/vars";

    private final Configuration configuration;
    private final TerminalAdapter terminal;
    private final Evaluator evaluator;
    private final Map<String, Consumer<Void>> handlers;
    private boolean continueLoop;

    public BallerinaShell(Configuration configuration, TerminalAdapter terminal) {
        this.configuration = configuration;
        this.terminal = terminal;
        this.continueLoop = true;
        this.evaluator = configuration.getEvaluator();

        // Register default handlers
        this.handlers = Map.of(
                RESET_COMMAND, v -> this.evaluator.reset(),
                HELP_COMMAND, v -> outputResource(HELP_FILE),
                TOGGLE_DEBUG, v -> this.configuration.toggleDebug(),
                STATE_COMMAND, v -> this.terminal.info(evaluator.toString()),
                IMPORTS_COMMAND, v -> this.terminal.info(evaluator.availableImports()),
                VARIABLES_COMMAND, v -> this.terminal.info(evaluator.availableVariables()),
                MODULE_DCLNS_COMMAND, v -> this.terminal.info(evaluator.availableModuleDeclarations()),
                EXIT_COMMAND, v -> {
                    this.continueLoop = false;
                    terminal.info(REPL_EXIT_MESSAGE);
                }
        );
    }

    /**
     * Runs the terminal application using the given config.
     */
    public void run() {
        String replPrompt = terminal.color(REPL_PROMPT, TerminalAdapter.GREEN);
        String banner = String.format(readFile(HEADER_FILE), REPL_VERSION);
        terminal.println(banner);

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
            String source = terminal.readLine(replPrompt, rightPrompt).trim();
            start = Instant.now();

            try {
                if (!source.isBlank()) {
                    if (this.handlers.containsKey(source)) {
                        this.handlers.get(source).accept(null);
                    } else {
                        String result = evaluator.evaluate(source);
                        if (result != null) {
                            terminal.result(result);
                        }
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
     * Read a resource file and output its content to the terminal.
     *
     * @param path File to read.
     */
    private void outputResource(@SuppressWarnings("SameParameterValue") String path) {
        String content = readFile(path);
        terminal.info(content);
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
}
