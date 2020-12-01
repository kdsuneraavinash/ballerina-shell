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


import io.ballerina.shell.cli.jline.FileKeywordsCompleter;
import io.ballerina.shell.cli.jline.JlineTerminalAdapter;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for REPL shell application.
 */
public class ReplShellApplication {
    private static final String TOPICS_FILE = "commands.help.topics.txt";
    private static final String KEYWORDS_FILE = "command.keywords.txt";
    private static final String COMMANDS_FILE = "command.keywords.txt";

    public static void execute(boolean isDebug, ApplicationConfiguration.EvaluatorMode mode) throws Exception {
        Configuration configuration = new ApplicationConfiguration(isDebug, mode);
        Terminal terminal = TerminalBuilder.terminal();
        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(DefaultParser.Bracket.CURLY,
                DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE);
        parser.setQuoteChars(new char[]{'"'});

        Map<String, Completer> completes = new HashMap<>();
        completes.put("HELP", new StringsCompleter("/help"));
        completes.put("HELP_ARGS", new FileKeywordsCompleter(TOPICS_FILE));
        completes.put("COMMANDS", new FileKeywordsCompleter(COMMANDS_FILE));
        completes.put("KEYWORDS", new FileKeywordsCompleter(KEYWORDS_FILE));
        Completer completer = new Completers.RegexCompleter(
                "HELP HELP_ARGS | COMMANDS | KEYWORDS*",
                completes::get);
        DefaultHighlighter highlighter = new DefaultHighlighter();

        LineReader lineReader = LineReaderBuilder.builder()
                .appName("Ballerina Shell REPL")
                .terminal(terminal)
                .completer(completer)
                .highlighter(highlighter)
                .parser(parser)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P > ")
                .variable(LineReader.INDENTATION, 2)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build();

        BallerinaShell shell = new BallerinaShell(configuration, new JlineTerminalAdapter(lineReader));
        shell.run();
    }

    public static void main(String[] args) throws Exception {
        ReplShellApplication.execute(false, ApplicationConfiguration.EvaluatorMode.CLASSLOAD);
    }
}
