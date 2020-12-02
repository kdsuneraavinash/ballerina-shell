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

package io.ballerina.shell.cli.handlers.help;

import io.ballerina.shell.cli.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Help provider that will fetch data from the BBE.
 * Will fetch the topic and show the description associated with them.
 * TODO: Replace with a proper help provider for REPL.
 */
public class RemoteBbeHelpProvider implements HelpProvider {
    private static final char HYPHEN = '-';
    private static final char UNDERSCORE = '_';

    private static final String NEWLINE = "\n";
    private static final String META_URL =
            "https://raw.githubusercontent.com/ballerina-platform/ballerina-distribution/master/examples/%s/";
    private static final String DESCRIPTION_FILE = "%s.description";
    private static final String EXAMPLE_FILE = "%s.bal";

    @Override
    public void getTopic(String[] args, StringBuilder output) throws HelpProviderException {
        boolean isExample = (args.length > 1) && args[1].equals("example");
        String topic = args[0];

        if (!topic.matches("^[a-zA-Z-_]+$")) {
            throw new HelpProviderException("Not a valid topic name.");
        }

        try {
            String metaUrl = String.format(META_URL, topic);
            String fileName = topic.replace(HYPHEN, UNDERSCORE);
            String file = String.format(isExample ? EXAMPLE_FILE : DESCRIPTION_FILE, fileName);
            String content = FileUtils.readFromUrl(metaUrl + file).trim();
            if (!isExample) {
                content = content
                        .replaceAll("[^|\n]//\\w*", NEWLINE) // Comment sign at start of lines
                        .replaceAll("<br/>", NEWLINE) // <br/> tag
                        .trim();
            }
            output.append(content).append(NEWLINE);
        } catch (FileNotFoundException e) {
            throw new HelpProviderException("" +
                    "No ballerina documentation found for '" + topic + "'.\n" +
                    "Use '/help TOPIC' to get help on a specific topic.");
        } catch (IOException e) {
            throw new HelpProviderException("" +
                    "Help retrieval failed.\n" +
                    "Use '/help TOPIC' to get help on a specific topic.");
        }
    }
}
