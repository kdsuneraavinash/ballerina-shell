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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.exceptions.TreeParserException;
import io.ballerina.shell.treeparser.trials.EmptyExpressionTrial;
import io.ballerina.shell.treeparser.trials.ExpressionTrial;
import io.ballerina.shell.treeparser.trials.ImportDeclarationTrial;
import io.ballerina.shell.treeparser.trials.ModuleMemberTrial;
import io.ballerina.shell.treeparser.trials.ParserTrialFailedException;
import io.ballerina.shell.treeparser.trials.StatementTrial;
import io.ballerina.shell.treeparser.trials.TreeParserTrial;

import java.util.Objects;

/**
 * Parses the source code line using a trial based method.
 * The source code is placed in several places and is attempted to parse.
 * This continues until the correct type can be determined.
 */
public class TrialTreeParser implements TreeParser {
    private static final TreeParserTrial[] TREE_PARSER_TRIALS = {
            new ImportDeclarationTrial(),
            new ExpressionTrial(),
            new StatementTrial(),
            new ModuleMemberTrial(),
            new EmptyExpressionTrial(),
    };

    @Override
    public Node parse(String source) throws TreeParserException {
        for (TreeParserTrial treeParserTrial : TREE_PARSER_TRIALS) {
            try {
                Node node = treeParserTrial.parse(source);
                Objects.requireNonNull(node, "Parser trial returned no nodes");
                return node;
            } catch (ParserTrialFailedException e) {
                PrinterProvider.debug(withClassName(treeParserTrial, e.getMessage()));
            } catch (Exception e) {
                PrinterProvider.debug(withClassName(treeParserTrial, "Something went wrong: " + e.getMessage()));
            }
        }
        throw new TreeParserException("Sorry, input statement not allowed.");
    }

    /**
     * Helper method to generate helpful error message.
     *
     * @param trial   Current running trial.
     * @param message Message to show as failed.
     * @return Trial name attached message.
     */
    private String withClassName(TreeParserTrial trial, String message) {
        return String.format("[%s failed] %s", trial.getClass().getSimpleName(), message);
    }
}
