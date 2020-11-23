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

import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 */
public class VariableDeclarationSnippet extends Snippet {
    public VariableDeclarationSnippet(ModuleVariableDeclarationNode rootNode) {
        super(SnippetSubKind.VARIABLE_DECLARATION, rootNode);
    }

    /**
     * Returns the root node of the variable declaration.
     * Root node must be a {@link ModuleVariableDeclarationNode}.
     * If the snippet was created through the constructor, this condition is satisfied.
     *
     * @return Root node of the syntax tree referring to this declaration.
     * @throws SnippetException If the snippet is invalid.
     */
    public ModuleVariableDeclarationNode getRootNode() throws SnippetException {
        if (rootNode instanceof ModuleVariableDeclarationNode) {
            return (ModuleVariableDeclarationNode) rootNode;
        }
        throw new SnippetException();
    }
}
