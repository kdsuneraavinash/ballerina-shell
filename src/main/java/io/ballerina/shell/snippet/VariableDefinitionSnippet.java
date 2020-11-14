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

package io.ballerina.shell.snippet;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 * TODO: Move variable value declaration into a statement snippet.
 */
public class VariableDefinitionSnippet extends Snippet {
    private final Node node;

    public VariableDefinitionSnippet(Node node, ExpressionNode initializer) {
        super(node.toSourceCode(), initializer == null
                ? SnippetSubKind.VARIABLE_DEFINITION_WITHOUT_VALUE
                : SnippetSubKind.VARIABLE_DEFINITION);
        this.node = node;
    }

    /**
     * Create a var definition snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static VariableDefinitionSnippet tryFromNode(Node node) {
        if (node instanceof ModuleVariableDeclarationNode) {
            ModuleVariableDeclarationNode declarationNode = (ModuleVariableDeclarationNode) node;
            return new VariableDefinitionSnippet(node,
                    declarationNode.initializer().orElse(null));

        } else if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode declarationNode = (VariableDeclarationNode) node;
            return new VariableDefinitionSnippet(node,
                    declarationNode.initializer().orElse(null));

        }

        return null;
    }

    /**
     * Gets the variable definition node associated with this snippet.
     *
     * @return Var definition node.
     */
    public Node getVariableNode() {
        return node;
    }
}
