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
import io.ballerina.shell.parser.TrialTreeParser;

/**
 * Trial to reject metadata and public keywords.
 * Will thrown runtime errors if invalid statements found.
 * Act as a way to detect errors faster and fail instead of lagging.
 */
public class RejectInvalidStmtTrial extends TreeParserTrial {
    private static final String DOCUMENTATION_START = "#";
    private static final String PUBLIC_START = "public";

    public RejectInvalidStmtTrial(TrialTreeParser parentParser) {
        super(parentParser);
    }

    @Override
    public Node parse(String source) throws ParserTrialFailedException {
        if (source.trim().startsWith(DOCUMENTATION_START)) {
            throw new ParserRejectedException("Documentation is not allowed in the REPL.");
        } else if (source.trim().startsWith(PUBLIC_START)) {
            throw new ParserRejectedException("Invalid qualifier 'public'. Public is not allowed here.");
        }
        throw new ParserTrialFailedException("Valid statement.");
    }
}
