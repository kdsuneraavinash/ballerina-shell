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

package io.ballerina.shell.test;

import com.google.gson.Gson;
import io.ballerina.shell.parser.TreeParser;
import io.ballerina.shell.parser.TrialTreeParser;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Scanner;

/**
 * Class with utility functions required for
 * other tests.
 */
public abstract class TestUtils {
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final long TEST_TREE_PARSER_TIMEOUT_MS = 500;

    /**
     * Loads a JSON fie with the given class format.
     *
     * @param fileName       JSON file to load.
     * @param testCasesClazz Class def of the required type.
     * @param <T>            Type of the return class.
     * @return The instance of the required type.
     */
    public static <T> T loadTestCases(String fileName, Class<T> testCasesClazz) {
        Gson gson = new Gson();
        InputStream inputStream = TestUtils.class.getClassLoader().getResourceAsStream(fileName);
        Objects.requireNonNull(inputStream, "Test file does not exist: " + fileName);
        try (Scanner scanner = new Scanner(inputStream, Charset.defaultCharset()).useDelimiter(SPECIAL_DELIMITER)) {
            String content = scanner.hasNext() ? scanner.next() : "";
            return gson.fromJson(content, testCasesClazz);
        }
    }

    /**
     * Creates a tree parser with extended timeout.
     *
     * @return Created tree parser.
     */
    public static TreeParser getTestTreeParser() {
        return new TrialTreeParser(TEST_TREE_PARSER_TIMEOUT_MS);
    }
}
