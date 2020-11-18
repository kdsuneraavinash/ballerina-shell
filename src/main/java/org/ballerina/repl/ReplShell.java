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

import io.ballerina.shell.BallerinaShell;
import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.postprocessor.BasicPostProcessor;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.preprocessor.CombinedPreprocessor;
import io.ballerina.shell.preprocessor.Preprocessor;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.transformer.CombinedTransformer;
import io.ballerina.shell.transformer.Transformer;
import io.ballerina.shell.treeparser.TreeParser;
import io.ballerina.shell.treeparser.TrialTreeParser;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Scanner;

/**
 * REPL shell terminal executor. Launches the terminal.
 */
public class ReplShell {
    private static final String HELP_FILE = "command.help.txt";
    private static final String IMPORTS_FILE = "command.imports.txt";
    private static final String HEADER_FILE = "command.header.txt";

    private static final String HELP_COMMAND = "help";
    private static final String IMPORTS_COMMAND = "imports";
    private static final String EXIT_COMMAND = "exit";
    private static final String TOGGLE_DEBUG = "debug";
    private static final String RESET_COMMAND = "reset";
    private static final String STATE_DUMP_COMMAND = "debug_state";

    private static final String REPL_PROMPT = "=$ ";
    private static final String REPL_EXIT_MESSAGE = "Bye!!";
    private static final String REPL_VERSION = "0.0.1";
    private static final String COMMAND_PREFIX = "/";

    private static final String SPECIAL_DELIMITER = "\\A";

    private final Terminal terminal;
    private final LineReader lineReader;
    private final BallerinaShell ballerinaShell;
    private final ReplConfiguration configuration;
    private final CommandHandler handler;
    private boolean continueLoop;

    public ReplShell(LineReader lineReader, ReplConfiguration configuration) {
        this.continueLoop = true;
        this.terminal = lineReader.getTerminal();
        this.lineReader = lineReader;
        this.configuration = configuration;
        this.handler = new CommandHandler(COMMAND_PREFIX);

        Preprocessor preprocessor = new CombinedPreprocessor(new SeparatorPreprocessor());
        TreeParser parser = new TrialTreeParser();
        Transformer transformer = new CombinedTransformer();
        Executor<?, ?, ?> executor = configuration.getExecutor();
        Postprocessor postprocessor = new BasicPostProcessor();
        this.ballerinaShell = new BallerinaShell(preprocessor, parser, transformer, executor, postprocessor);

    }

    /**
     * Execute the REPL.
     * This would show the welcome banner, take user input and evaluate it.
     * To exit the repl, {@code ReplExitException} should be thrown.
     */
    public void execute() {
        // 1. Output welcome banner
        // 2. Attach handlers
        // 3. In each iteration,
        //      a. Read and handle internal commands
        //      b. If not handled, evaluate line
        // ^C will be ignored.
        // The time taken for execution is recorded.

        String banner = readFile(HEADER_FILE);
        banner = String.format(banner, REPL_VERSION);
        terminal.writer().println(banner);
        attachCommandHandlers();
        Duration previousDuration = null;

        while (continueLoop) {
            try {
                String line = readInput(previousDuration);
                Instant start = Instant.now();
                try {
                    if (line.trim().isBlank()) {
                        continue;
                    }
                    if (handler.handle(line, terminal.writer())) {
                        continue;
                    }
                    ballerinaShell.evaluate(line.trim());
                } finally {
                    Instant end = Instant.now();
                    previousDuration = Duration.between(start, end);
                }
            } catch (UserInterruptException | EndOfFileException ignored) {
            } catch (Exception e) {
                PrinterProvider.debug(e.toString());
                String message = colored(e.getMessage(), AttributedStyle.RED);
                terminal.writer().println(message);
                configuration.printStackTrace(e);
                terminal.writer().flush();
            }
        }
        terminal.writer().println(REPL_EXIT_MESSAGE);
        terminal.writer().flush();
    }

    /**
     * Reads a line from user. Duration for last execution is in right side.
     *
     * @param previousDuration Duration for the last statement evaluation.
     * @return User input.
     */
    private String readInput(Duration previousDuration) {
        String rightPrompt = null;
        if (previousDuration != null) {
            long seconds = previousDuration.toMillis();
            rightPrompt = colored("took " + seconds + "ms", AttributedStyle.BRIGHT);
        }
        return lineReader.readLine(REPL_PROMPT, rightPrompt, (Character) null, null);
    }

    /**
     * Attach the command handlers for internal command.
     */
    private void attachCommandHandlers() {
        this.handler.attachHandler(EXIT_COMMAND, (w) -> this.continueLoop = false);
        this.handler.attachHandler(IMPORTS_COMMAND, (w) -> outputResource(IMPORTS_FILE, w));
        this.handler.attachHandler(HELP_COMMAND, (w) -> outputResource(HELP_FILE, w));
        this.handler.attachHandler(TOGGLE_DEBUG, (w) -> configuration.toggleDiagnosticOutputMode());
        this.handler.attachHandler(RESET_COMMAND, (w) -> ballerinaShell.reset());
        this.handler.attachHandler(STATE_DUMP_COMMAND, (w) -> ballerinaShell.dumpState());
    }

    /**
     * Reads the header file content from the resources.
     *
     * @return Read text.
     */
    private static String readFile(String path) {
        ClassLoader classLoader = ReplShellApplication.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        Objects.requireNonNull(inputStream, "File does not exist: " + path);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }

    /**
     * Read a resource file and output its content to the terminal.
     *
     * @param path   File to read.
     * @param writer Writer to use to output.
     */
    private static void outputResource(String path, PrintWriter writer) {
        String content = readFile(path);
        writer.println(colored(content, AttributedStyle.BRIGHT));
    }

    /**
     * Colors a text by the given color.
     *
     * @param text  String to color.
     * @param color Color to use.
     * @return Colored string.
     */
    public static String colored(String text, int color) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(color))
                .append(text).toAnsi();
    }
}
