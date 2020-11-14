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
import io.ballerina.shell.exceptions.ParserException;
import io.ballerina.shell.treeparser.trials.EmptyExpressionTrial;
import io.ballerina.shell.treeparser.trials.ExpressionTrial;
import io.ballerina.shell.treeparser.trials.ImportDeclarationTrial;
import io.ballerina.shell.treeparser.trials.ModuleMemberTrial;
import io.ballerina.shell.treeparser.trials.ParserTrialFailedException;
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

    @Override
    public Node parse(String source) {
        for (TreeParserTrial treeParserTrial : TREE_PARSER_TRIALS) {
            try {
                return treeParserTrial.tryParse(source);
            } catch (ParserTrialFailedException ignored) {
                // Trial failed, try next trial
            }
        }

        throw new ParserException("Sorry, input statement not allowed.");
    }
}
