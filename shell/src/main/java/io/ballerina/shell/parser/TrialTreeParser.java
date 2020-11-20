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

package io.ballerina.shell.parser;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.exceptions.TreeParserException;
import io.ballerina.shell.parser.trials.EmptyExpressionTrial;
import io.ballerina.shell.parser.trials.ExpressionTrial;
import io.ballerina.shell.parser.trials.ImportDeclarationTrial;
import io.ballerina.shell.parser.trials.ModuleMemberTrial;
import io.ballerina.shell.parser.trials.StatementTrial;
import io.ballerina.shell.parser.trials.TreeParserTrial;

import java.util.List;
import java.util.Objects;

/**
 * Parses the source code line using a trial based method.
 * The source code is placed in several places and is attempted to parse.
 * This continues until the correct type can be determined.
 */
public class TrialTreeParser extends TreeParser {
    private static final List<TreeParserTrial> NODE_PARSER_TRIALS = List.of(
            new ImportDeclarationTrial(),
            new ExpressionTrial(),
            new StatementTrial(),
            new ModuleMemberTrial(),
            new EmptyExpressionTrial()
    );

    @Override
    public Node parse(String source) throws TreeParserException {
        for (TreeParserTrial trial : NODE_PARSER_TRIALS) {
            try {
                Node node = trial.parse(source);
                Objects.requireNonNull(node, "trial returned no nodes");
                return node;
            } catch (Exception e) {
                String message = String.format("Failed %s because %s",
                        trial.getClass().getSimpleName(), e.getMessage());
                addDiagnostic(Diagnostic.debug(message));
            }
        }
        addDiagnostic(Diagnostic.error("" +
                "Invalid statement. Could not parse the expression."));
        throw new TreeParserException();
    }
}
