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

package io.ballerina.shell.preprocessor;

import io.ballerina.shell.PrinterProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines several preprocessors into a single preprocessor.
 * The strings would be processed by preprocessors in the order provided.
 */
public class CombinedPreprocessor implements Preprocessor {
    private final Preprocessor[] preprocessors;

    /**
     * Create a combined preprocessor by the given preprocessors.
     *
     * @param preprocessors All the preprocessors to combine.
     */
    public CombinedPreprocessor(Preprocessor... preprocessors) {
        // Send a debug message of preprocessors
        PrinterProvider.debug("Attached preprocessors: " + preprocessors.length);
        this.preprocessors = preprocessors;
    }

    @Override
    public List<String> preprocess(String input) {
        List<String> strings = List.of(input);
        for (Preprocessor preprocessor : preprocessors) {
            List<String> processed = new ArrayList<>();
            for (String string : strings) {
                processed.addAll(preprocessor.preprocess(string));
            }
            strings = processed;
        }
        return strings;
    }
}
