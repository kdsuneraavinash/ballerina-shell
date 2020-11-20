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
import io.ballerina.shell.invoker.replay.ReplayInvoker;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;

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

    private final Configuration configuration;
    private final TerminalAdapter terminal;
    private final Evaluator evaluator;
    private final Map<String, Consumer<String>> handlers;
    private boolean continueLoop;

    public BallerinaShell(Configuration configuration, TerminalAdapter terminal) {
        this.configuration = configuration;
        this.terminal = terminal;
        this.continueLoop = true;
        this.evaluator = createEvaluator();

        // Register default handlers
        this.handlers = Map.of(
                HELP_COMMAND, s -> outputResource(HELP_FILE),
                EXIT_COMMAND, s -> continueLoop = false,
                TOGGLE_DEBUG, s -> configuration.toggleDebug(),
                RESET_COMMAND, s -> evaluator.reset(),
                STATE_COMMAND, s -> terminal.writer().println(terminal.color(evaluator.toString(), TerminalAdapter.CYAN))
        );
    }

    /**
     * Runs the terminal application using the given config.
     */
    public void run() {
        String banner = String.format(readFile(HEADER_FILE), REPL_VERSION);
        terminal.writer().println(banner);

        Duration previousDuration = Duration.ZERO;
        while (continueLoop) {
            String rightPrompt = String.format("took %s ms", previousDuration.toMillis());
            String source = terminal.readLine(REPL_PROMPT, rightPrompt).trim();
            Instant start = Instant.now();

            try {
                if (!source.isBlank()) {
                    if (this.handlers.containsKey(source)) {
                        this.handlers.get(source).accept(source);
                    } else {
                        evaluator.evaluate(source);
                    }
                }
            } catch (Exception e) {
                if (!evaluator.hasErrors()) {
                    terminal.writer().println(terminal.color("Invalid syntax.", TerminalAdapter.RED));
                }
                outputException(e);
            } finally {
                Instant end = Instant.now();
                previousDuration = Duration.between(start, end);
                evaluator.diagnostics().forEach(this::outputDiagnostic);
                evaluator.resetDiagnostics();
            }

        }
        terminal.writer().println(REPL_EXIT_MESSAGE);
        terminal.writer().flush();
    }

    /**
     * Creates and returns an evaluator based on the config.
     *
     * @return Created evaluator.
     */
    private Evaluator createEvaluator() {
        if (configuration.getEvaluator() == Configuration.EvaluatorType.REPLAY) {
            Evaluator evaluator = new Evaluator();
            evaluator.setPreprocessor(new SeparatorPreprocessor());
            evaluator.setTreeParser(new TrialTreeParser());
            evaluator.setSnippetFactory(new BasicSnippetFactory());
            evaluator.setInvoker(new ReplayInvoker("template.replay.ftl", "main.bal"));
            return evaluator;
        }
        throw new RuntimeException("Unknown evaluator type.");
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
        terminal.writer().println(terminal.color(content, TerminalAdapter.BRIGHT));
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

        int color = TerminalAdapter.BRIGHT;
        if (diagnostic.getKind() == DiagnosticKind.ERROR) {
            color = TerminalAdapter.RED;
        } else if (diagnostic.getKind() == DiagnosticKind.WARN) {
            color = TerminalAdapter.YELLOW;
        }
        terminal.writer().println(terminal.color(diagnostic.toString(), color));
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
            String stackTrace = terminal.color(stringWriter.toString(),
                    TerminalAdapter.RED | TerminalAdapter.BRIGHT);
            terminal.writer().println(stackTrace);
        }
        terminal.writer().flush();
    }
}
