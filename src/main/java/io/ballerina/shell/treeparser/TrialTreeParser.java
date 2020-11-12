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
package io.ballerina.shell.treeparser;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.shell.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.treeparser.trials.EmptyExpressionTrial;
import io.ballerina.shell.treeparser.trials.ExpressionTrial;
import io.ballerina.shell.treeparser.trials.FailedTrialException;
import io.ballerina.shell.treeparser.trials.ImportDeclarationTrial;
import io.ballerina.shell.treeparser.trials.ModuleMemberTrial;
import io.ballerina.shell.treeparser.trials.StatementTrial;
import io.ballerina.shell.treeparser.trials.TreeParserTrial;

/**
 * Parses the source code line using a trial based method.
 * The source code is placed in several places and is attempted to parse.
 * This continues until the correct type can be determined.
 */
public class TrialTreeParser implements TreeParser {
    private static final TreeParserTrial[] TREE_PARSER_TRIALS = {
            new ExpressionTrial(),
            new StatementTrial(),
            new ModuleMemberTrial(),
            new EmptyExpressionTrial(),
            new ImportDeclarationTrial(),
    };

    public TrialTreeParser() {
        ShellDiagnosticProvider.sendMessage("Using trial parser to parse tree.");
        StringBuilder message = new StringBuilder();
        message.append("Attached ").append(TREE_PARSER_TRIALS.length).append(" tree parser trials: ");
        for (TreeParserTrial treeParserTrial : TREE_PARSER_TRIALS) {
            message.append(treeParserTrial.getClass().getSimpleName()).append(" ");
        }
        ShellDiagnosticProvider.sendMessage(message.toString());
    }

    @Override
    public Node parse(String source) {
        assert source.endsWith(";");

        for (TreeParserTrial treeParserTrial : TREE_PARSER_TRIALS) {
            try {
                return treeParserTrial.tryParse(source);
            } catch (FailedTrialException ignored) {
                // Trial failed, try next trial
            }
        }

        throw new RuntimeException("[Parsing Failed] Error in input: " + source);
    }

    /**
     * Creates a nil literal node. ()
     * This is the default placeholder expression.
     *
     * @return A new nil literal node
     */
    public static ExpressionNode nilLiteralExpression() {
        return NodeFactory.createNilLiteralNode(
                NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN)
        );
    }
}
