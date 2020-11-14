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

package io.ballerina.shell.treeparser.trials;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

/**
 * Attempts to capture a empty expression.
 * This could be a comment, white space, etc...
 * Puts in the module level and checks for empty module level entry.
 */
public class EmptyExpressionTrial implements TreeParserTrial {
    @Override
    public Node tryParse(String source) throws ParserTrialFailedException {
        try {
            TextDocument document = TextDocuments.from(source);
            SyntaxTree tree = SyntaxTree.from(document);
            ModulePartNode node = tree.rootNode();

            NodeList<ModuleMemberDeclarationNode> moduleMemberDeclarationNodes = node.members();
            if (moduleMemberDeclarationNodes.isEmpty()) {
                return nilLiteralExpression();
            }
            throw new Exception("Not an empty expression");
        } catch (Exception e) {
            throw new ParserTrialFailedException(e);
        }
    }

    /**
     * Creates a nil literal node. ()
     * This is the default placeholder expression.
     *
     * @return A new nil literal node
     */
    private static ExpressionNode nilLiteralExpression() {
        return NodeFactory.createNilLiteralNode(
                NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN)
        );
    }
}
