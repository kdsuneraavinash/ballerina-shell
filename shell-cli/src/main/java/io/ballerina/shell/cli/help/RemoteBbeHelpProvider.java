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

package io.ballerina.shell.cli.help;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Help provider that will fetch data from the BBE.
 * Will fetch the topic and show the description associated with them.
 * TODO: Replace with a proper help provider for REPL.
 */
public class RemoteBbeHelpProvider implements HelpProvider {
    private static final char HYPHEN = '-';
    private static final char UNDERSCORE = '_';
    private static final String NEWLINE = "\n";
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String META_URL =
            "https://raw.githubusercontent.com/ballerina-platform/ballerina-distribution/master/examples/%s/";
    private static final String DESCRIPTION_FILE = "%s.description";

    @Override
    public void getTopic(String topic, StringBuilder output) {
        try {
            String metaUrl = String.format(META_URL, topic);
            String descriptionFile = String.format(DESCRIPTION_FILE, topic.replace(HYPHEN, UNDERSCORE));
            String description = getLinkContent(metaUrl + descriptionFile).trim()
                    .replaceAll("^// ", NEWLINE)
                    .replaceAll("\n// ", NEWLINE)
                    .replaceAll("<br/>", NEWLINE)
                    .trim();
            output.append(description).append(NEWLINE);

        } catch (FileNotFoundException e) {
            output.append("No ballerina documentation found for '")
                    .append(topic).append("'").append(NEWLINE);
            output.append("Use '/help TOPIC' to get help on a specific topic.");
        } catch (IOException e) {
            output.append("Help retrieval failed.").append(NEWLINE);
            output.append(e);
        }
    }

    private String getLinkContent(String link) throws IOException {
        URL url = new URL(link);
        InputStream inputStream = url.openStream();
        Scanner scanner = new Scanner(inputStream, Charset.defaultCharset()).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }
}
