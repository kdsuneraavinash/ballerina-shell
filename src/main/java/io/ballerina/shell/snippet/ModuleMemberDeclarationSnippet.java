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

import io.ballerina.compiler.syntax.tree.AnnotationDeclarationNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleXMLNamespaceDeclarationNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

/**
 * Module level declarations.
 * These are not active or runnable except for service declarations.
 * Any undefined variable in these declarations are ignored.
 * (Except for module level variable declarations)
 */
public class ModuleMemberDeclarationSnippet extends Snippet<ModuleMemberDeclarationNode> {
    protected ModuleMemberDeclarationSnippet(ModuleMemberDeclarationNode node, SnippetSubKind subKind) {
        super(node, subKind);
        assert subKind.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND;
    }

    /**
     * Create a module member declaration snippet from the given node.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ModuleMemberDeclarationSnippet fromNode(ModuleMemberDeclarationNode node) {
        if (node instanceof FunctionDefinitionNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.FUNCTION_DEFINITION);
        } else if (node instanceof ListenerDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.LISTENER_DECLARATION);
        } else if (node instanceof TypeDefinitionNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.TYPE_DEFINITION);
        } else if (node instanceof ServiceDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.SERVICE_DECLARATION);
        } else if (node instanceof ConstantDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.CONSTANT_DECLARATION);
        } else if (node instanceof ModuleVariableDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.MODULE_VARIABLE_DECLARATION);
        } else if (node instanceof AnnotationDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.ANNOTATION_DECLARATION);
        } else if (node instanceof ModuleXMLNamespaceDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.MODULE_XML_NAMESPACE_DECLARATION);
        } else if (node instanceof EnumDeclarationNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.ENUM_DECLARATION);
        } else if (node instanceof ClassDefinitionNode) {
            return new ModuleMemberDeclarationSnippet(node, SnippetSubKind.CLASS_DEFINITION);
        } else {
            throw new IllegalArgumentException("Node is of unexpected type");
        }
    }
}
