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

import io.ballerina.compiler.syntax.tree.AnnotationDeclarationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleXMLNamespaceDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.compiler.syntax.tree.XMLNamespaceDeclarationNode;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.types.ExpressionSnippet;
import io.ballerina.shell.snippet.types.ImportDeclarationSnippet;
import io.ballerina.shell.snippet.types.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.types.StatementSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;

import java.util.Map;

/**
 * A static factory that will create snippets from given nodes.
 */
public class BasicSnippetFactory extends SnippetFactory {
    // TODO: Instead of booleans, map to subkinds later.
    // Create a caches of Syntax kind -> Variable types/Sub snippets that are known.
    // These will be used when identifying the variable type/snippet type.
    protected static final Map<Class<?>, Boolean> MODULE_MEM_DCLNS = Map.ofEntries(
            Map.entry(FunctionDefinitionNode.class, true),
            Map.entry(ListenerDeclarationNode.class, true),
            Map.entry(TypeDefinitionNode.class, true),
            Map.entry(ServiceDeclarationNode.class, false), // Error
            Map.entry(ConstantDeclarationNode.class, true),
            Map.entry(AnnotationDeclarationNode.class, true),
            Map.entry(ModuleXMLNamespaceDeclarationNode.class, true),
            Map.entry(EnumDeclarationNode.class, true),
            Map.entry(ClassDefinitionNode.class, true)
    );
    protected static final Map<Class<?>, Boolean> STATEMENTS = Map.ofEntries(
            Map.entry(AssignmentStatementNode.class, true),
            Map.entry(CompoundAssignmentStatementNode.class, true),
            Map.entry(VariableDeclarationNode.class, false), // Ignore
            Map.entry(BlockStatementNode.class, true),
            Map.entry(BreakStatementNode.class, false), // Error
            Map.entry(FailStatementNode.class, false), // Error
            Map.entry(ExpressionStatementNode.class, false), // Ignore
            Map.entry(ContinueStatementNode.class, false), // Error
            Map.entry(IfElseStatementNode.class, true),
            Map.entry(WhileStatementNode.class, true),
            Map.entry(PanicStatementNode.class, true),
            Map.entry(ReturnStatementNode.class, false), // Error
            Map.entry(LocalTypeDefinitionStatementNode.class, false), // Ignore
            Map.entry(LockStatementNode.class, true),
            Map.entry(ForkStatementNode.class, true),
            Map.entry(ForEachStatementNode.class, true),
            Map.entry(XMLNamespaceDeclarationNode.class, false), // Ignore
            Map.entry(TransactionStatementNode.class, true),
            Map.entry(RollbackStatementNode.class, false), // Error
            Map.entry(RetryStatementNode.class, true),
            Map.entry(MatchStatementNode.class, true),
            Map.entry(DoStatementNode.class, true)
    );

    @Override
    public ImportDeclarationSnippet createImportSnippet(Node node) {
        if (node instanceof ImportDeclarationNode) {
            return new ImportDeclarationSnippet((ImportDeclarationNode) node);
        }
        return null;
    }

    @Override
    public VariableDeclarationSnippet createVariableDeclarationSnippet(Node node) {
        ModuleVariableDeclarationNode dclnNode;
        if (node instanceof ModuleVariableDeclarationNode) {
            dclnNode = (ModuleVariableDeclarationNode) node;
        } else if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varNode = (VariableDeclarationNode) node;
            NodeList<Token> qualifiers = NodeFactory.createEmptyNodeList();
            if (varNode.finalKeyword().isPresent()) {
                qualifiers = NodeFactory.createNodeList(varNode.finalKeyword().get());
            }
            // TODO: Inject variable default value or reject ones without initializers.
            if (varNode.initializer().isEmpty()) {
                addDiagnostic(Diagnostic.warn("Variables without initializers are not permitted."));
            }
            dclnNode = NodeFactory.createModuleVariableDeclarationNode(
                    NodeFactory.createMetadataNode(null, varNode.annotations()),
                    qualifiers, varNode.typedBindingPattern(),
                    varNode.equalsToken().orElse(null), varNode.initializer().orElse(null),
                    varNode.semicolonToken()
            );
        } else {
            return null;
        }
        // TODO: Validate variable name is not reserved.
        return new VariableDeclarationSnippet(dclnNode);
    }

    @Override
    public ModuleMemberDeclarationSnippet createModuleMemberDeclarationSnippet(Node node) {
        if (node instanceof ModuleMemberDeclarationNode) {
            assert MODULE_MEM_DCLNS.containsKey(node.getClass());
            if (MODULE_MEM_DCLNS.get(node.getClass())) {
                return new ModuleMemberDeclarationSnippet((ModuleMemberDeclarationNode) node);
            }
        }
        return null;
    }

    @Override
    public StatementSnippet createStatementSnippet(Node node) {
        if (node instanceof StatementNode) {
            assert STATEMENTS.containsKey(node.getClass());
            if (STATEMENTS.get(node.getClass())) {
                return new StatementSnippet((StatementNode) node);
            }
        }
        return null;
    }

    @Override
    public Snippet createExpressionSnippet(Node node) {
        // TODO: Add all subtypes later.
        if (node instanceof ExpressionNode) {
            return new ExpressionSnippet((ExpressionNode) node);
        }
        return null;
    }
}
