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
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.XmlTypeDescriptorNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.util.Optional;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 * TODO: Move variable value declaration into a statement snippet.
 */
public class VariableDeclarationSnippet extends Snippet {
    public VariableDeclarationSnippet(String sourceCode) {
        super(sourceCode, SnippetSubKind.VARIABLE_DECLARATION);
    }

    /**
     * Create a var definition snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static VariableDeclarationSnippet tryFromNode(Node node) {
        Optional<ExpressionNode> originalInitializer;
        TypedBindingPatternNode bind;
        SyntaxKind possibleType;

        if (node instanceof ModuleVariableDeclarationNode) {
            ModuleVariableDeclarationNode declarationNode = (ModuleVariableDeclarationNode) node;
            // TODO: Decide on accepting these.
            assertTrue(declarationNode.metadata().isEmpty(),
                    "No metadata allowed for variables in the REPL.");
            assertTrue(declarationNode.finalKeyword().isEmpty(),
                    "Variables cannot be final in the REPL");
            bind = declarationNode.typedBindingPattern();
            originalInitializer = declarationNode.initializer();
        } else if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode declarationNode = (VariableDeclarationNode) node;
            // TODO: Decide on accepting these.
            assertTrue(declarationNode.annotations().isEmpty(),
                    "Annotations are not allowed for variables in the REPL.");
            assertTrue(declarationNode.finalKeyword().isEmpty(),
                    "Variables cannot be final in the REPL");
            bind = declarationNode.typedBindingPattern();
            originalInitializer = declarationNode.initializer();
        } else {
            return null;
        }

        // Find the type and try to infer the type.
        // There can be several types of variable declarations.
        // 1. int i = 0     -- has initializer, can infer default
        // 2. int i         -- no initializer, can infer default
        // 3. error i = f() -- has initializer, cant infer default
        // 4. error i       -- no initializer, cant infer default
        // Type 4 is rejected. Type 3 will be converted into type 2.
        // In the conversion it would be taken as if initializer IS the default.
        // Only 1 can be broken into 2 statements. Its canBreakIntoStatements will be set to true.
        possibleType = findPossibleType(bind);
        Optional<String> defaultInitializer = getDefaultInitializer(possibleType);

        Optional<String> inferredInitializer = originalInitializer.isEmpty()
                ? defaultInitializer : Optional.of(originalInitializer.get().toSourceCode());
        String debugMsg = String.format("Type for variable: %s. Initializer is %s.", possibleType, inferredInitializer);
        DebugProvider.sendMessage(debugMsg);

        assertTrue(inferredInitializer.isPresent(), "" +
                "Initializer is required for variable declarations of this type.\n" +
                "REPL will infer most of the types' default values, but this type could not be inferred.");
        String sourceCode = String.format("%s = %s;", bind.toSourceCode(), inferredInitializer.get());
        return new VariableDeclarationSnippet(sourceCode);
    }

    /**
     * Get the syntax kind associated with the node.
     *
     * @param type Node to get syntax kind of.
     * @return Kind that is associated with node. Null if failed.
     */
    private static SyntaxKind findPossibleType(Node type) {
        if (type instanceof NilTypeDescriptorNode) { // nil
            return SyntaxKind.NIL_TYPE_DESC;
        } else if (type instanceof XmlTypeDescriptorNode) { // xml
            return SyntaxKind.XML_TYPE_DESC;
        } else if (type instanceof ArrayTypeDescriptorNode) { // array
            return SyntaxKind.ARRAY_TYPE_DESC;
        } else if (type instanceof TupleTypeDescriptorNode) { // tuple
            return SyntaxKind.TUPLE_TYPE_DESC;

        } else if (type instanceof ParenthesisedTypeDescriptorNode) { // (type)
            return findPossibleType(((ParenthesisedTypeDescriptorNode) type).typedesc());
        } else if (type instanceof OptionalTypeDescriptorNode) { // optional
            return SyntaxKind.OPTIONAL_TYPE_DESC;
        } else if (type instanceof UnionTypeDescriptorNode) { // union
            SyntaxKind leftType = findPossibleType(((UnionTypeDescriptorNode) type).leftTypeDesc());
            SyntaxKind rightType = findPossibleType(((UnionTypeDescriptorNode) type).rightTypeDesc());
            return leftType == null ? rightType : leftType;
        } else if (type instanceof IntersectionTypeDescriptorNode) { // intersection
            SyntaxKind leftType = findPossibleType(((IntersectionTypeDescriptorNode) type).leftTypeDesc());
            SyntaxKind rightType = findPossibleType(((IntersectionTypeDescriptorNode) type).rightTypeDesc());
            return leftType == null || rightType == null ? null : leftType;

        } else if (type instanceof SimpleNameReferenceNode) {
            return findPossibleType(((SimpleNameReferenceNode) type).name());
        } else if (type instanceof BuiltinSimpleNameReferenceNode) {
            return findPossibleType(((BuiltinSimpleNameReferenceNode) type).name());

        } else if (type instanceof TypedBindingPatternNode) {
            return findPossibleType(((TypedBindingPatternNode) type).typeDescriptor());
        } else if (type instanceof Token) {
            SyntaxKind kind = type.kind();
            if (kind == SyntaxKind.NIL_TYPE_DESC || kind == SyntaxKind.INT_KEYWORD
                    || kind == SyntaxKind.FLOAT_KEYWORD || kind == SyntaxKind.BOOLEAN_KEYWORD
                    || kind == SyntaxKind.DECIMAL_KEYWORD || kind == SyntaxKind.STRING_KEYWORD
                    || kind == SyntaxKind.XML_TYPE_DESC || kind == SyntaxKind.ARRAY_TYPE_DESC
                    || kind == SyntaxKind.TUPLE_TYPE_DESC || kind == SyntaxKind.JSON_KEYWORD
                    || kind == SyntaxKind.ANY_KEYWORD || kind == SyntaxKind.OPTIONAL_TYPE_DESC
                    || kind == SyntaxKind.ANYDATA_KEYWORD || kind == SyntaxKind.BYTE_KEYWORD) {
                return type.kind();
            }
        }
        return null;
    }

    /**
     * Get a possible default initializer.
     * This can be used to initialize the variable.
     * TODO: Integrate with a Parser API to parse string to an expression.
     *
     * @param type Type to get the initializer.
     * @return A string representation of default initializer.
     */
    private static Optional<String> getDefaultInitializer(SyntaxKind type) {
        if (type == SyntaxKind.NIL_TYPE_DESC || type == SyntaxKind.JSON_KEYWORD
                || type == SyntaxKind.ANY_KEYWORD || type == SyntaxKind.OPTIONAL_TYPE_DESC
                || type == SyntaxKind.ANYDATA_KEYWORD) {
            return Optional.of("()");
        } else if (type == SyntaxKind.BYTE_KEYWORD || type == SyntaxKind.INT_KEYWORD
                || type == SyntaxKind.FLOAT_KEYWORD || type == SyntaxKind.DECIMAL_KEYWORD) {
            return Optional.of("0");
        } else if (type == SyntaxKind.BOOLEAN_KEYWORD) {
            return Optional.of("false");
        } else if (type == SyntaxKind.STRING_KEYWORD) {
            return Optional.of("\"\"");
        } else if (type == SyntaxKind.XML_TYPE_DESC) {
            return Optional.of("xml `<xml></xml>`");
        } else if (type == SyntaxKind.ARRAY_TYPE_DESC || type == SyntaxKind.TUPLE_TYPE_DESC) {
            return Optional.of("[]");
        }
        return Optional.empty();
    }
}
