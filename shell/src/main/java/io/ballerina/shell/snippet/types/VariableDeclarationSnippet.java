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

import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.Pair;

import java.util.List;
import java.util.Optional;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 */
public class VariableDeclarationSnippet extends Snippet {
    public VariableDeclarationSnippet(ModuleVariableDeclarationNode rootNode) {
        super(SnippetSubKind.VARIABLE_DECLARATION, rootNode);
    }

    /**
     * Finds the variable names and types in the snippet.
     * If the type cannot be determined, (var) it is ignored.
     * The found vars are returned as a list of pairs of var name and type.
     *
     * @return Variable name and types found.
     */
    public List<Pair<String, String>> findVariableNamesAndTypes() {
        ModuleVariableDeclarationNode declarationNode = (ModuleVariableDeclarationNode) rootNode;
        if (declarationNode.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode) {
            // If array with asterisk, cannot infer: int[*] a = 1212;
            if (isAsteriskArrayDef(declarationNode.typedBindingPattern().typeDescriptor())) {
                return List.of();
            }
            // If var type, cannot infer: var x = 1212;
            if (isVarDef(declarationNode.typedBindingPattern().typeDescriptor())) {
                return List.of();
            }
            // Otherwise we can infer.
            String variableName = ((CaptureBindingPatternNode) declarationNode.typedBindingPattern().bindingPattern())
                    .variableName().text().trim();
            String variableType = declarationNode.typedBindingPattern().typeDescriptor().toString().trim();
            return List.of(new Pair<>(variableName, variableType));
        }
        return List.of();
    }

    private boolean isVarDef(TypeDescriptorNode typeDescriptorNode) {
        return typeDescriptorNode instanceof BuiltinSimpleNameReferenceNode
                && typeDescriptorNode.kind() == SyntaxKind.VAR_TYPE_DESC;
    }

    private boolean isAsteriskArrayDef(TypeDescriptorNode typeDescriptorNode) {
        if (typeDescriptorNode instanceof ArrayTypeDescriptorNode) {
            Optional<Node> arrayLength = ((ArrayTypeDescriptorNode) typeDescriptorNode).arrayLength();
            if (arrayLength.isPresent()) {
                if (arrayLength.get() instanceof BasicLiteralNode) {
                    return arrayLength.get().kind() == SyntaxKind.ASTERISK_LITERAL;
                }
            }
        }
        return false;
    }
}
