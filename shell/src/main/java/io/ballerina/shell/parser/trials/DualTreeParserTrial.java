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

package io.ballerina.shell.parser.trials;

import io.ballerina.compiler.syntax.tree.Node;

/**
 * Trial which is run with and without trailing semicolon.
 */
public abstract class DualTreeParserTrial extends TreeParserTrial {
    private static final String SEMICOLON = ";";

    @Override
    public Node parse(String source) throws ParserTrialFailedException {
        try {
            return parseSource(source);
        } catch (ParserTrialFailedException e) {
            if (source.endsWith(SEMICOLON)) {
                return parseSource(source.substring(0, source.length() - 1));
            }
            throw e;
        }
    }

    public abstract Node parseSource(String source) throws ParserTrialFailedException;
}
