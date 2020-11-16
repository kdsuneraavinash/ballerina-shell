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

import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.compiler.syntax.tree.XMLNamespaceDeclarationNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;

/**
 * These are normal statements that should be evaluated from
 * top to bottom inside a function.
 */
public class StatementSnippet extends Snippet {
    protected StatementSnippet(Node node, SnippetSubKind subKind) {
        super(node.toSourceCode(), subKind);
    }

    protected StatementSnippet(String sourceCode, SnippetSubKind subKind) {
        super(sourceCode, subKind);
    }

    /**
     * Create an assignment snippet from given source code.
     * The code must be a assignment snippet verified by the callee.
     * No additional check will be done from this method.
     *
     * @param sourceCode Code corresponding to the assignment.
     * @return Snippet that contains the code.
     */
    public static StatementSnippet fromCodeOfAssignment(String sourceCode) {
        return new StatementSnippet(sourceCode, SnippetSubKind.ASSIGNMENT_STATEMENT_SUBKIND);
    }


    /**
     * Create a statement snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static StatementSnippet tryFromNode(Node node) {
        if (node instanceof AssignmentStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.ASSIGNMENT_STATEMENT_SUBKIND);
        } else if (node instanceof CompoundAssignmentStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.COMPOUND_ASSIGNMENT_STATEMENT_SUBKIND);
        } else if (node instanceof VariableDeclarationNode) {
            return new StatementSnippet(node, SnippetSubKind.VARIABLE_DECLARATION_STATEMENT);
        } else if (node instanceof BlockStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.BLOCK_STATEMENT);
        } else if (node instanceof BreakStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.BREAK_STATEMENT);
        } else if (node instanceof FailStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.FAIL_STATEMENT);
        } else if (node instanceof ExpressionStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.EXPRESSION_STATEMENT);
        } else if (node instanceof ContinueStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.CONTINUE_STATEMENT);
        } else if (node instanceof IfElseStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.IF_ELSE_STATEMENT);
        } else if (node instanceof WhileStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.WHILE_STATEMENT);
        } else if (node instanceof PanicStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.PANIC_STATEMENT);
        } else if (node instanceof ReturnStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.RETURN_STATEMENT);
        } else if (node instanceof LocalTypeDefinitionStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.LOCAL_TYPE_DEFINITION_STATEMENT);
        } else if (node instanceof LockStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.LOCK_STATEMENT);
        } else if (node instanceof ForkStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.FORK_STATEMENT);
        } else if (node instanceof ForEachStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.FOR_EACH_STATEMENT);
        } else if (node instanceof XMLNamespaceDeclarationNode) {
            return new StatementSnippet(node, SnippetSubKind.XML_NAMESPACE_DECLARATION_STATEMENT);
        } else if (node instanceof TransactionStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.TRANSACTION_STATEMENT);
        } else if (node instanceof RollbackStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.ROLLBACK_STATEMENT);
        } else if (node instanceof RetryStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.RETRY_STATEMENT);
        } else if (node instanceof MatchStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.MATCH_STATEMENT);
        } else if (node instanceof DoStatementNode) {
            return new StatementSnippet(node, SnippetSubKind.DO_STATEMENT);
        }
        return null;
    }
}

