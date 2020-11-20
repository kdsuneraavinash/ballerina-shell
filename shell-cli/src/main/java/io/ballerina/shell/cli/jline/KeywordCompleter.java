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

import org.jline.reader.impl.completer.StringsCompleter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Simple completer for the REPL which completes Ballerina Keywords.
 * Keywords are read off of a text file.
 */
public class KeywordCompleter extends StringsCompleter {
    private static final String KEYWORDS_FILE = "command.keywords.txt";

    public KeywordCompleter() {
        super(readKeywords());
    }

    /**
     * Read the keywords file and return a list of keywords.
     *
     * @return Keywords list.
     */
    public static List<String> readKeywords() {
        ClassLoader classLoader = KeywordCompleter.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(KEYWORDS_FILE);
        Objects.requireNonNull(inputStream, "Keyword file does not exist.");
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(",");

        List<String> keywords = new ArrayList<>();
        while (scanner.hasNext()) {
            String keyword = scanner.next().trim();
            if (keyword.isBlank()) {
                continue;
            }
            keywords.add(keyword);
        }
        return keywords;
    }
}
