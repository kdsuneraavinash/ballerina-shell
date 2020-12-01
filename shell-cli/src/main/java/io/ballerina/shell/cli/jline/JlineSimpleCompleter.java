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

package io.ballerina.shell.cli.jline;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;

/**
 * A simple completer to give completions based on the input line.
 * If the input starts with /, built-in commands are given.
 * Otherwise keyword completion is given.
 */
public class JlineSimpleCompleter implements Completer {
    private static final String TOPICS_FILE = "commands.help.topics.txt";
    private static final String KEYWORDS_FILE = "command.keywords.txt";
    private static final String COMMANDS_FILE = "command.commands.txt";

    private final StringsCompleter topicsCompleter;
    private final StringsCompleter topicsOptionCompleter;
    private final StringsCompleter commandsCompleter;
    private final StringsCompleter keywordsCompleter;

    public JlineSimpleCompleter() {
        this.topicsCompleter = new FileKeywordsCompleter(TOPICS_FILE);
        this.topicsOptionCompleter = new StringsCompleter("description", "example");
        this.commandsCompleter = new FileKeywordsCompleter(COMMANDS_FILE);
        this.keywordsCompleter = new FileKeywordsCompleter(KEYWORDS_FILE);
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line.line().trim().startsWith("/help")) {
            if (line.wordIndex() == 1) {
                topicsCompleter.complete(reader, line, candidates);
            } else {
                topicsOptionCompleter.complete(reader, line, candidates);
            }
        } else if (line.line().trim().startsWith("/")) {
            commandsCompleter.complete(reader, line, candidates);
        } else {
            keywordsCompleter.complete(reader, line, candidates);
        }

    }
}
