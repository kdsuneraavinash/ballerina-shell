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
package org.ballerina.shell;

import io.ballerina.shell.BallerinaShell;
import io.ballerina.shell.diagnostics.ShellDiagnosticProvider;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;

/**
 * Ballerina base shell REPL.
 * Executes a interactive shell to let the user interact with Ballerina Shell.
 */
public class BallerinaReplShell {
    private static final String REPL_HEADER =
            "Welcome to Ballerina Shell REPL\n" +
                    "Exit by pressing Ctrl+C";
    private static final String REPL_PROMPT = ">> ";
    private static final String REPL_EXIT_MESSAGE = "Bye!!";
    private static final String DEBUG_KEYWORD = "debug";

    private final Terminal terminal;
    private final LineReader lineReader;
    private final BallerinaShell ballerinaShell;

    public BallerinaReplShell() throws IOException {
        this.terminal = TerminalBuilder.terminal();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal)
                .parser(new DefaultParser()).build();
        this.ballerinaShell = new BallerinaShell();
    }

    /**
     * Execute the REPL.
     */
    public void execute() {
        terminal.writer().println(REPL_HEADER);
        while (true) {
            try {
                String line = lineReader.readLine(REPL_PROMPT);
                ReplResultController shellResult = new ReplResultController(terminal);
                ballerinaShell.evaluate(line.trim(), shellResult);

            } catch (UserInterruptException | EndOfFileException e) {
                terminal.writer().println(REPL_EXIT_MESSAGE);
                break;
            } catch (Exception e) {
                String message = new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
                        .append((e.getMessage()))
                        .toAnsi();
                terminal.writer().println(message);
                terminal.writer().flush();
            }
        }

        terminal.writer().flush();
    }


    /**
     * Read the optional args and launch the REPL.
     *
     * @param args Optional arguments.
     * @throws IOException If terminal initialization failed.
     */
    public static void main(String[] args) throws IOException {
        boolean outputDiagnostics = args.length > 0
                && args[0].equalsIgnoreCase(DEBUG_KEYWORD);
        BallerinaReplShell replShell = new BallerinaReplShell();
        if (outputDiagnostics) {
            ShellDiagnosticProvider.getInstance().setWriter(new ReplDiagnosticWriter(replShell.terminal));
            ShellDiagnosticProvider.sendMessage("Diagnostic output mode is ON.");
        }
        replShell.execute();
    }
}
