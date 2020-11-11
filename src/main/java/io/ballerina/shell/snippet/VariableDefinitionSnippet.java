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

import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 * TODO: Move variable value declaration into a statement snippet.
 */
public class VariableDefinitionSnippet extends Snippet<Node> {
    public VariableDefinitionSnippet(Node node, SnippetSubKind subKind) {
        super(node, subKind);
    }

    /**
     * Create a var definition snippet from the given node.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static VariableDefinitionSnippet fromNode(Node node) {
        if (node instanceof ModuleVariableDeclarationNode) {
            if (((ModuleVariableDeclarationNode) node).initializer().isEmpty()) {
                return new VariableDefinitionSnippet(node, SnippetSubKind.VARIABLE_DEFINITION_WITHOUT_VALUE);
            }
            return new VariableDefinitionSnippet(node, SnippetSubKind.VARIABLE_DEFINITION);
        } else if (node instanceof VariableDeclarationNode) {
            if (((VariableDeclarationNode) node).initializer().isEmpty()) {
                return new VariableDefinitionSnippet(node, SnippetSubKind.VARIABLE_DEFINITION_WITHOUT_VALUE);
            }
            return new VariableDefinitionSnippet(node, SnippetSubKind.VARIABLE_DEFINITION);
        } else {
            throw new IllegalArgumentException("Node is of unexpected type");
        }
    }
}
