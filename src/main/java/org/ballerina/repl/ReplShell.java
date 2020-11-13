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
import io.ballerina.shell.ExecutorFailedException;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;
import org.ballerina.repl.exceptions.ReplExitException;
import org.ballerina.repl.exceptions.ReplHandledException;
import org.ballerina.repl.exceptions.ReplToggleDebugException;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Scanner;

/**
 * REPL shell terminal executor. Launches the terminal.
 */
public class ReplShell {
    private static final String REPL_HEADER = "header.txt";
    private static final String REPL_PROMPT = "=$ ";
    private static final String REPL_EXIT_MESSAGE = "Bye!!";
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String VERSION = "0.0.1";

    private final Terminal terminal;
    private final LineReader lineReader;
    private final BallerinaShell ballerinaShell;
    private final ReplConfiguration configuration;

    public ReplShell(LineReader lineReader, ReplConfiguration configuration) {
        this.terminal = lineReader.getTerminal();
        this.lineReader = lineReader;
        this.configuration = configuration;
        this.ballerinaShell = new BallerinaShell(configuration.getExecutor());
    }

    /**
     * Execute the REPL.
     */
    public void execute() {
        ReplResultController replResultController = new ReplResultController(terminal);

        // Output welcome banner
        String banner = readFile(REPL_HEADER);
        banner = String.format(banner, VERSION);
        terminal.writer().println(banner);

        Duration previousDuration = null;
        while (true) {
            try {
                String line = readInput(lineReader, previousDuration);
                ReplCommandHandler.handle(line, terminal.writer());
                Instant start = Instant.now();
                try {
                    ballerinaShell.evaluate(line.trim(), replResultController);
                } catch (ExecutorFailedException ignored) {
                } finally {
                    Instant end = Instant.now();
                    previousDuration = Duration.between(start, end);
                }
            } catch (ReplExitException e) {
                terminal.writer().println(REPL_EXIT_MESSAGE);
                break;
            } catch (ReplToggleDebugException e) {
                configuration.toggleDiagnosticOutputMode();
            } catch (UserInterruptException | EndOfFileException | ReplHandledException ignored) {
                // ignore
            } catch (Exception e) {
                ShellDiagnosticProvider.sendMessage(e.toString());
                String message = new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                        .append((e.getMessage())).toAnsi();
                terminal.writer().println(message);
                configuration.printStackTrace(e);
                terminal.writer().flush();
            }
        }

        terminal.writer().flush();
    }

    private static String readInput(LineReader lineReader, Duration previousDuration) {
        String rightPrompt = null;
        if (previousDuration != null) {
            long seconds = previousDuration.toMillis();
            rightPrompt = new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT))
                    .append("took ").append(String.valueOf(seconds)).append("ms").toAnsi();
        }
        return lineReader.readLine(REPL_PROMPT, rightPrompt, (Character) null, null);
    }

    /**
     * Reads the header file content from the resources.
     *
     * @return Read text.
     */
    public static String readFile(String path) {
        ClassLoader classLoader = ReplShellExecutor.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        Objects.requireNonNull(inputStream, "File does not exist: " + path);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }
}
