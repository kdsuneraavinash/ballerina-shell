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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.ballerina.repl.exceptions.ReplCmdHelpException;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * Ballerina base shell REPL.
 * Executes a interactive shell to let the user interact with Ballerina Shell.
 */
public class ReplShellExecutor {
    private static final String HELP_MESSAGE = "./run.sh [OPTIONS]";

    /**
     * Read the optional args and launch the REPL.
     *
     * @param args Optional arguments.
     * @throws IOException If terminal initialization failed.
     */
    public static void main(String[] args) throws IOException, ParseException {
        Options options = ReplConfiguration.getCommandLineOptions();
        CommandLineParser commandLineParser = new org.apache.commons.cli.DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        ReplConfiguration configuration;
        Terminal terminal = TerminalBuilder.terminal();

        try {
            cmd = commandLineParser.parse(options, args);
            configuration = new ReplConfiguration(cmd, terminal);
        } catch (ParseException e) {
            formatter.printHelp(HELP_MESSAGE, options);
            terminal.writer().println(e.getMessage());
            return;
        } catch (ReplCmdHelpException e) {
            formatter.printHelp(HELP_MESSAGE, options);
            return;
        }

        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY,
                DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE);
        Completer completer = new ReplKeywordCompleter();
        DefaultHighlighter highlighter = new DefaultHighlighter();

        LineReader lineReader = LineReaderBuilder.builder()
                .appName("Ballerina Shell REPL")
                .terminal(terminal)
                .completer(completer)
                .highlighter(highlighter)
                .parser(parser)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build();

        ReplShell replShell = new ReplShell(lineReader, configuration);
        replShell.execute();
    }
}
