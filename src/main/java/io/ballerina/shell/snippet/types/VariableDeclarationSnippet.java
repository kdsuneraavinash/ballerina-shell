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
import io.ballerina.compiler.syntax.tree.DistinctTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ErrorTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterizedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SingletonTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TableTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceTypeDescNode;
import io.ballerina.compiler.syntax.tree.TypedescTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.XmlTypeDescriptorNode;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 * TODO: Move variable value declaration into a statement snippet.
 */
public class VariableDeclarationSnippet extends Snippet {
    private static final Map<SyntaxKind, VariableType> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put(SyntaxKind.NIL_TYPE_DESC, VariableType.NIL);
        TYPE_MAP.put(SyntaxKind.INT_KEYWORD, VariableType.NUMERIC);
        TYPE_MAP.put(SyntaxKind.FLOAT_KEYWORD, VariableType.NUMERIC);
        TYPE_MAP.put(SyntaxKind.BOOLEAN_KEYWORD, VariableType.BOOLEAN);
        TYPE_MAP.put(SyntaxKind.DECIMAL_KEYWORD, VariableType.NUMERIC);
        TYPE_MAP.put(SyntaxKind.STRING_KEYWORD, VariableType.STRING);
        TYPE_MAP.put(SyntaxKind.XML_TYPE_DESC, VariableType.XML);
        TYPE_MAP.put(SyntaxKind.ARRAY_TYPE_DESC, VariableType.LIST);
        TYPE_MAP.put(SyntaxKind.TUPLE_TYPE_DESC, VariableType.LIST);
        TYPE_MAP.put(SyntaxKind.JSON_KEYWORD, VariableType.JSON_ANYDATA);
        TYPE_MAP.put(SyntaxKind.ANY_KEYWORD, VariableType.ANY);
        TYPE_MAP.put(SyntaxKind.OPTIONAL_TYPE_DESC, VariableType.OPTIONAL);
        TYPE_MAP.put(SyntaxKind.ANYDATA_KEYWORD, VariableType.JSON_ANYDATA);
        TYPE_MAP.put(SyntaxKind.BYTE_KEYWORD, VariableType.NUMERIC);
        TYPE_MAP.put(SyntaxKind.MAP_KEYWORD, VariableType.MAP);
    }

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
        // Only variables that are allowed are module level variables.
        // So, all the variable declarations are converted into module
        // level variable declarations. However, there might be other
        // variable declarations as well. If that is the case attempt to
        // parse it as a module level variable by removing/adding some
        // additional information.
        // Binding pattern is also required to be analyzed so that
        // we can synthesize if the type is serializable.
        // Also some declarations might not give a initializer.
        // These are not allowed in module level. Using type we can
        // infer a default value.
        // # Module level declarations available children:
        //      metadata, finalKeyword, typedBindingPattern,
        //      equalsToken, initializer, semicolonToken
        // # Variable declarations available children:
        //      annotations, finalKeyword, typedBindingPattern,
        //      equalsToken, initializer, semicolonToken
        // annotations are valid metadata. So, inferring is doable.

        ModuleVariableDeclarationNode dclnNode;
        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varDcln = (VariableDeclarationNode) node;
            // TODO: If initializer is null, infer a value if possible
            dclnNode = NodeFactory.createModuleVariableDeclarationNode(
                    NodeFactory.createMetadataNode(null, varDcln.annotations()),
                    varDcln.finalKeyword().orElse(null), varDcln.typedBindingPattern(),
                    varDcln.equalsToken().orElse(null), varDcln.initializer().orElse(null),
                    varDcln.semicolonToken()
            );
        } else if (node instanceof ModuleVariableDeclarationNode) {
            dclnNode = (ModuleVariableDeclarationNode) node;
        } else {
            return null;
        }

        // Find the type and try to infer the type.
        // There can be several types of variable declarations.
        // 1. int i = 0     -- has initializer, can infer default
        // 2. int i         -- no initializer, can infer default
        // 3. error i = f() -- has initializer, cant infer default
        // 4. error i       -- no initializer, cant infer default
        // In these ones, (1) and (3) does not need inferring.
        // (2) can be inferred. (4) is rejected.
        // So we need to infer only if the type is a doable type and
        // no initializer present.

        if (dclnNode.initializer().isPresent()) {
            return new VariableDeclarationSnippet(dclnNode.toSourceCode());
        }

        VariableType type = identifyType(dclnNode.typedBindingPattern().typeDescriptor());
        if (type.getDefaultValue().isEmpty()) {
            // If inferring failed as well, throw an error message.
            throw new SnippetException("Initializer is required for variable declarations of this type.\n" +
                    "REPL will infer most of the types' default values, but this type could not be inferred.");
        }

        if (type.isWarned()) {
            // TODO: Warned property should be propagated
            DebugProvider.sendMessage("Inferring type of a warned type");
        }
        // Try to insert a token with the default value text as a identifier
        // and get the source code. This should be equal to the expected one.
        // Since initializer is null, equal sign must be null as well.
        // So we need to provide it.
        String sourceCode = NodeFactory.createModuleVariableDeclarationNode(
                dclnNode.metadata().orElse(null), dclnNode.finalKeyword().orElse(null),
                dclnNode.typedBindingPattern(), NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN),
                type.getDefaultValue().get(), dclnNode.semicolonToken()
        ).toSourceCode();

        return new VariableDeclarationSnippet(sourceCode);
    }

    /**
     * Identify the variable type associated with the type.
     *
     * @param type Node to get syntax kind of.
     * @return Kind that is associated with node. UNIDENTIFIED if failed.
     */
    private static VariableType identifyType(TypeDescriptorNode type) {
        if (type instanceof ErrorTypeDescriptorNode
                || type instanceof FunctionTypeDescriptorNode
                || type instanceof StreamTypeDescriptorNode
                || type instanceof ObjectTypeDescriptorNode
                || type instanceof TypedescTypeDescriptorNode) {
            return VariableType.BEHAVIORAL;
        } else if (type instanceof SingletonTypeDescriptorNode
                || type instanceof IntersectionTypeDescriptorNode
                || type instanceof DistinctTypeDescriptorNode) {
            return VariableType.OTHER;
        } else if (type instanceof TableTypeDescriptorNode) {
            return VariableType.TABLE;
        } else if (type instanceof XmlTypeDescriptorNode) {
            return VariableType.XML;
        } else if (type instanceof RecordTypeDescriptorNode) {
            return VariableType.RECORD;
        } else if (type instanceof ArrayTypeDescriptorNode
                || type instanceof TupleTypeDescriptorNode) {
            return VariableType.LIST;
        } else if (type instanceof NilTypeDescriptorNode) {
            return VariableType.NIL;
        } else if (type instanceof TypeReferenceTypeDescNode) {
            return identifyType(((TypeReferenceTypeDescNode) type).typeRef());
        } else if (type instanceof ParenthesisedTypeDescriptorNode) {
            return identifyType(((ParenthesisedTypeDescriptorNode) type).typedesc());
        } else if (type instanceof BuiltinSimpleNameReferenceNode) {
            return identifyType(((BuiltinSimpleNameReferenceNode) type).name());
        } else if (type instanceof SimpleNameReferenceNode) {
            return identifyType(((SimpleNameReferenceNode) type).name());
        } else if (type instanceof OptionalTypeDescriptorNode) {
            // Optional cannot be inferred directly
            // Serializable if all types serializable.
            // Default is ()
            Node typeDescriptor = ((OptionalTypeDescriptorNode) type).typeDescriptor();
            VariableType rest;
            if (typeDescriptor instanceof TypeDescriptorNode) {
                rest = identifyType((TypeDescriptorNode) typeDescriptor);
            } else if (typeDescriptor instanceof Token) {
                rest = identifyType((Token) typeDescriptor);
            } else {
                rest = VariableType.UNIDENTIFIED;
            }
            return new VariableType(rest.isSerializable(), VariableType.OPTIONAL.getDefaultValue().orElse(null));
        } else if (type instanceof UnionTypeDescriptorNode) {
            // Union cannot be inferred directly
            // Union default cannot be determined. We need internal types for that.
            //  Serializable if all types serializable.
            //  Default value of any type with a default.
            VariableType leftType = identifyType(((UnionTypeDescriptorNode) type).leftTypeDesc());
            VariableType rightType = identifyType(((UnionTypeDescriptorNode) type).rightTypeDesc());
            ExpressionNode defaultType = leftType.getDefaultValue().orElse(rightType.getDefaultValue().orElse(null));
            boolean isSerializable = VariableType.UNION_TYPE.isSerializable()
                    && leftType.isSerializable() && rightType.isSerializable();
            return new VariableType(isSerializable, defaultType);
        } else if (type instanceof ParameterizedTypeDescriptorNode) {
            // Something like map<string>
            // Parameterized type as well as type parameter should be serializable
            // Default would be the default for parameterized type. (if any)
            // Eg: to be serializable, map and string should be serializable.
            //     default would be the default of map.
            ParameterizedTypeDescriptorNode paramTypeDes = (ParameterizedTypeDescriptorNode) type;
            VariableType paramType = identifyType(paramTypeDes.parameterizedType());
            VariableType typeParam = identifyType(paramTypeDes.typeParameter().typeNode());
            return new VariableType(typeParam.isSerializable() && paramType.isSerializable(),
                    paramType.getDefaultValue().orElse(null));
        } else if (type instanceof QualifiedNameReferenceNode
                || type instanceof IndexedExpressionNode
                || type instanceof TypeReferenceNode) {
            return VariableType.UNIDENTIFIED;
        }
        return VariableType.UNIDENTIFIED;
    }

    /**
     * Identify the variable type associated with the type.
     *
     * @param type Node to get syntax kind of.
     * @return Kind that is associated with node. UNIDENTIFIED if failed.
     */
    private static VariableType identifyType(Token type) {
        return TYPE_MAP.getOrDefault(type.kind(), VariableType.UNIDENTIFIED);
    }
}
