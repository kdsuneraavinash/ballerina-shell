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
package io.ballerina.shell.transformer;

import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.shell.snippet.ErroneousSnippet;
import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.ImportSnippet;
import io.ballerina.shell.snippet.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.StatementSnippet;
import io.ballerina.shell.snippet.VariableDefinitionSnippet;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Transforms the syntax tree into the corresponding snippet.
 * Priority is as {@code ImportSnippet}, {@code VariableDefinitionSnippet},
 * {@code ExpressionSnippet}, {@code ModuleMemberDeclarationSnippet} and {@code StatementSnippet}.
 */
public class SyntaxTreeTransformer implements Transformer<Node, Snippet<?>> {
    private static final Logger LOGGER = Logger.getLogger(SyntaxTreeTransformer.class.getName());

    @Override
    public Snippet<?> transform(Node value) {
        LOGGER.log(Level.INFO, "Identified type: " + value.getClass().getSimpleName());

        if (value instanceof ImportDeclarationNode) {
            return new ImportSnippet((ImportDeclarationNode) value);
        } else if (value instanceof ModuleVariableDeclarationNode) {
            // VariableDeclarationNode is also here. But they are currently rejected from parser.
            // TODO: Accept VariableDeclarationNode as well.
            return new VariableDefinitionSnippet((ModuleVariableDeclarationNode) value);
        } else if (value instanceof ExpressionStatementNode) {
            return new ExpressionSnippet((ExpressionStatementNode) value);
        } else if (value instanceof ModuleMemberDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(((ModuleMemberDeclarationNode) value));
        } else if (value instanceof StatementNode) {
            return new StatementSnippet((StatementNode) value);
        } else {
            return new ErroneousSnippet(value);
        }
    }
}
