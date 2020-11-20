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

package io.ballerina.shell.snippet.factory;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.DiagnosticReporter;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.types.ImportDeclarationSnippet;
import io.ballerina.shell.snippet.types.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.types.StatementSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class SnippetFactory extends DiagnosticReporter {
    /**
     * Creates a snippet from the given node.
     * This will throw and error if the resultant snippet is an erroneous snippet.
     * TODO: Add error message throwing for invalid type of nodes.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     * @throws SnippetException If couldn't identify the snippet.
     */
    public Snippet createSnippet(Node node) throws SnippetException {
        List<Function<Node, Snippet>> functions = new ArrayList<>();
        functions.add(this::createImportSnippet);
        functions.add(this::createVariableDeclarationSnippet);
        functions.add(this::createModuleMemberDeclarationSnippet);
        functions.add(this::createStatementSnippet);
        functions.add(this::createExpressionSnippet);
        Snippet snippet;
        for (Function<Node, Snippet> function : functions) {
            snippet = function.apply(node);
            if (snippet != null) {
                String message = String.format("Node identified as a %s snippet.", snippet.getKind());
                addDiagnostic(Diagnostic.debug(message));
                return snippet;
            }
        }
        addDiagnostic(Diagnostic.error("" +
                "Sorry couldn't identify the expression. " +
                "Check your expression for syntax errors."));
        throw new SnippetException();
    }

    /**
     * Create a import snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public abstract ImportDeclarationSnippet createImportSnippet(Node node);

    /**
     * Create a variable declaration snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public abstract VariableDeclarationSnippet createVariableDeclarationSnippet(Node node);

    /**
     * Create a module member declaration snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public abstract ModuleMemberDeclarationSnippet createModuleMemberDeclarationSnippet(Node node);

    /**
     * Create a statement snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public abstract StatementSnippet createStatementSnippet(Node node);

    /**
     * Create a expression snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public abstract Snippet createExpressionSnippet(Node node);
}