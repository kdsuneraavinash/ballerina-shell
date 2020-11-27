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
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

/**
 * Trial for testing for the correct syntax tree.
 */
public abstract class TreeParserTrial {
    /**
     * Tries to parse the source into a syntax tree.
     * Returns null if failed.
     *
     * @param source Input source statement.
     * @return Parsed syntax tree root node. Null if failed.
     * @throws ParserTrialFailedException If trial failed.
     */
    public abstract Node parse(String source) throws ParserTrialFailedException;

    /**
     * Checks for errors in the syntax tree.
     *
     * @param tree Tree to check.
     * @throws ParserTrialFailedException If tree contains errors.
     */
    protected void assertTree(SyntaxTree tree) throws ParserTrialFailedException {
        for (Diagnostic diagnostic : tree.diagnostics()) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                throw new ParserTrialFailedException(tree.textDocument(), diagnostic);
            }
        }
    }

    /**
     * Helper assertion to throw if condition is not satisfied.
     *
     * @param condition Condition to check.
     * @param message   Error message if failed.
     * @throws ParserTrialFailedException If condition is not satisfied.
     */
    protected void assertIf(boolean condition, String message) throws ParserTrialFailedException {
        if (!condition) {
            throw new ParserTrialFailedException(message);
        }
    }
}
