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

package io.ballerina.shell.snippet.types;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.NodeUtilities;

import java.util.Objects;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 */
public class VariableDeclarationSnippet extends Snippet {
    private final ModuleVariableDeclarationNode moduleVariableDeclarationNode;
    private final String variableName;
    private final boolean isSerializable;

    public VariableDeclarationSnippet(ModuleVariableDeclarationNode dclnNode,
                                      String variableName, boolean isSerializable) {
        super(dclnNode.toSourceCode(), SnippetSubKind.VARIABLE_DECLARATION);
        this.variableName = variableName;
        this.isSerializable = isSerializable;
        this.moduleVariableDeclarationNode = dclnNode;
    }

    /**
     * Recreate the snippet with a different initializer.
     *
     * @param initializer New initializer string representation. Should be an expression.
     * @return Copied snippet.
     */
    public VariableDeclarationSnippet withInitializer(String initializer) {
        ExpressionNode initializerNode = NodeUtilities.tryToParseExpression(initializer);
        Objects.requireNonNull(initializerNode, "New initializer is not a valid initializer.");
        ModuleVariableDeclarationNode newDclnNode = this.moduleVariableDeclarationNode
                .modify().withInitializer(initializerNode).apply();
        return new VariableDeclarationSnippet(newDclnNode, this.variableName, this.isSerializable);
    }

    public String getVariableName() {
        return variableName;
    }

    public boolean isSerializable() {
        return isSerializable;
    }
}
