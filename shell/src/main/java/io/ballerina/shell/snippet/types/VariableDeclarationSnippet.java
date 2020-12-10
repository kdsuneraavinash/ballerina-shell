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
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.DiagnosticReporter;
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
    /**
     * A node visitor that will check whether a variable declaration contains any
     * compile time bindings (such as var, int[*],...). If so, isInfferrable will be
     * set to false after visitor was applied.
     */
    private static class InferrableDecideVisitor extends NodeVisitor {
        boolean isInferrable;

        InferrableDecideVisitor() {
            isInferrable = true;
        }

        @Override
        public void visit(ArrayTypeDescriptorNode arrayTypeDescriptorNode) {
            super.visit(arrayTypeDescriptorNode);
            Optional<Node> arrayLength = arrayTypeDescriptorNode.arrayLength();
            if (arrayLength.isPresent()) {
                if (arrayLength.get() instanceof BasicLiteralNode) {
                    isInferrable = false;
                }
            }
        }

        @Override
        public void visit(BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
            super.visit(builtinSimpleNameReferenceNode);
            if (builtinSimpleNameReferenceNode.kind() == SyntaxKind.VAR_TYPE_DESC) {
                isInferrable = false;
            }
        }
    }


    public VariableDeclarationSnippet(ModuleVariableDeclarationNode rootNode) {
        super(SnippetSubKind.VARIABLE_DECLARATION, rootNode);
    }

    /**
     * Creates a new snippet where initializer is not provided.
     *
     * @return Var dcln snippet without the initializer (RHS).
     */
    public VariableDeclarationSnippet withoutInitializer() {
        assert rootNode instanceof ModuleVariableDeclarationNode;
        return new VariableDeclarationSnippet(((ModuleVariableDeclarationNode) rootNode).modify()
                .withInitializer(null).apply());
    }

    /**
     * Finds the variable names and types in the snippet.
     * If the type cannot be determined, (var) it is ignored.
     * The found vars are returned as a list of pairs of var name and type.
     *
     * @return Variable name and types found.
     */
    public List<Pair<String, String>> findVariableNamesAndTypes(DiagnosticReporter reporter) {
        ModuleVariableDeclarationNode declarationNode = (ModuleVariableDeclarationNode) withoutInitializer().rootNode;

        // If not captured binding patterns, skip.
        if (!(declarationNode.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode)) {
            reporter.addDiagnostic(Diagnostic.warn("" +
                    "Warning, only captured binding patterns are fully supported at the moment."));
            return List.of();
        }

        // If not inferrable, skip.
        InferrableDecideVisitor visitor = new InferrableDecideVisitor();
        declarationNode.accept(visitor);
        if (!visitor.isInferrable) {
            return List.of();
        }

        CaptureBindingPatternNode bindingPatternNode = (CaptureBindingPatternNode) declarationNode
                .typedBindingPattern().bindingPattern();
        return List.of(new Pair<>(bindingPatternNode.variableName().text(),
                declarationNode.typedBindingPattern().typeDescriptor().toSourceCode()));
    }
}
