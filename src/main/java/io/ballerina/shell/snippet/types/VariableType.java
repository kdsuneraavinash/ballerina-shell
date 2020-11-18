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
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
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
import io.ballerina.compiler.syntax.tree.XmlTypeDescriptorNode;
import io.ballerina.shell.PrinterProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Variable type describes the type of a variable.
 * This also defines whether the type is serializable or not.
 */
public class VariableType {
    // () value
    private static final ExpressionNode NIL_LITERAL_NODE = ExpressionSnippet.fromStringToExpression("()");
    // false value
    private static final ExpressionNode FALSE_LITERAL_NODE = ExpressionSnippet.fromStringToExpression("false");
    // 0 value
    private static final ExpressionNode ZERO_LITERAL_NODE = ExpressionSnippet.fromStringToExpression("0");
    // "" value
    private static final ExpressionNode EMPTY_STRING_LITERAL_NODE = ExpressionSnippet.fromStringToExpression("\"\"");
    // xml `<!---->` value
    private static final ExpressionNode EMPTY_XML_NODE = ExpressionSnippet.fromStringToExpression("xml `<!---->`");
    // [] value
    private static final ExpressionNode EMPTY_LIST_NODE = ExpressionSnippet.fromStringToExpression("[]");
    // {} value
    private static final ExpressionNode EMPTY_MAP_NODE = ExpressionSnippet.fromStringToExpression("{}");
    // table[] value
    private static final ExpressionNode EMPTY_TABLE_NODE = ExpressionSnippet.fromStringToExpression("table []");

    // () - basic, simple
    public static final VariableType NIL = new VariableType(true, NIL_LITERAL_NODE);
    // true, false - basic, simple
    public static final VariableType BOOLEAN = new VariableType(true, FALSE_LITERAL_NODE);
    // integers, floats, double and byte - basic, simple
    public static final VariableType NUMERIC = new VariableType(true, ZERO_LITERAL_NODE);

    // a sequence of Unicode scalar values - basic, sequence
    public static final VariableType STRING = new VariableType(true, EMPTY_STRING_LITERAL_NODE);
    // a sequence of zero or more elements, processing instructions,
    // comments or text items - basic, sequence
    public static final VariableType XML = new VariableType(true, EMPTY_XML_NODE);

    // array/tuple, an ordered list of values - basic, structured
    public static final VariableType LIST = new VariableType(true, EMPTY_LIST_NODE, true);
    // a mapping from keys, which are strings, to values;
    // specifies mappings in terms of a single type to which all keys
    // are mapped - basic, structured
    public static final VariableType MAP = new VariableType(true, EMPTY_MAP_NODE, true);
    // a mapping from keys, which are strings, to values;
    // specifies maps in terms of names of fields (required keys)
    // and value for each field - basic, structured
    // TODO: Is this serializable?
    public static final VariableType RECORD = new VariableType(false, EMPTY_MAP_NODE, true);
    // a ordered collection of mappings, where a mapping is uniquely
    // identified within the table by a key derived from the mapping - basic, structured
    // TODO: Cannot serialize because of key?
    public static final VariableType TABLE = new VariableType(false, EMPTY_TABLE_NODE);

    // Any behavioral type cannot be persisted, neither has a default value
    public static final VariableType BEHAVIORAL = new VariableType(false);

    // union cannot be checked directly. - other
    public static final VariableType UNION_TYPE = new VariableType(true);
    // a value that is either () or belongs to a type - other
    public static final VariableType OPTIONAL = new VariableType(false, NIL_LITERAL_NODE);
    // any value other than an error - other
    // any cannot be serialized because any can be assigned with anything.
    public static final VariableType ANY = new VariableType(false, NIL_LITERAL_NODE);
    // union type that can be serialized - other
    //  json or anydata
    public static final VariableType JSON_ANYDATA = new VariableType(true, NIL_LITERAL_NODE);
    // other types that cannot be serialized nor have a default - other
    //  intersection, singleton, readonly, never, distinct
    public static final VariableType OTHER = new VariableType(false);
    // Any un-identifiable type - can be user defined, etc...
    public static final VariableType UNIDENTIFIED = new VariableType(false);

    private static final Map<SyntaxKind, VariableType> TYPE_MAP = new HashMap<>();

    // Create a cache of Syntax kind -> Variable types that are known.
    // This will be used when identifying the variable type.
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

    private final boolean isSerializable;
    private final ExpressionNode defaultValue;
    private final boolean warned;

    public VariableType(boolean isSerializable) {
        this(isSerializable, null, false);
    }

    public VariableType(boolean isSerializable, ExpressionNode defaultValue) {
        this(isSerializable, defaultValue, false);
    }

    public VariableType(boolean isSerializable, ExpressionNode defaultValue, boolean warned) {
        this.isSerializable = isSerializable;
        this.defaultValue = defaultValue;
        this.warned = warned;
    }

    /**
     * Identify the variable type associated with the type.
     *
     * @param type Node to get syntax kind of.
     * @return Kind that is associated with node. UNIDENTIFIED if failed.
     */
    public static VariableType fromDescriptor(TypeDescriptorNode type) {
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
            return fromDescriptor(((TypeReferenceTypeDescNode) type).typeRef());
        } else if (type instanceof ParenthesisedTypeDescriptorNode) {
            return fromDescriptor(((ParenthesisedTypeDescriptorNode) type).typedesc());
        } else if (type instanceof BuiltinSimpleNameReferenceNode) {
            return fromToken(((BuiltinSimpleNameReferenceNode) type).name());
        } else if (type instanceof SimpleNameReferenceNode) {
            return fromToken(((SimpleNameReferenceNode) type).name());
        } else if (type instanceof OptionalTypeDescriptorNode) {
            // Optional cannot be inferred directly
            // Serializable if all types serializable.
            // Default is ()
            Node typeDescriptor = ((OptionalTypeDescriptorNode) type).typeDescriptor();
            VariableType rest;
            if (typeDescriptor instanceof TypeDescriptorNode) {
                rest = fromDescriptor((TypeDescriptorNode) typeDescriptor);
            } else if (typeDescriptor instanceof Token) {
                rest = fromToken((Token) typeDescriptor);
            } else {
                rest = VariableType.UNIDENTIFIED;
            }
            ExpressionNode defaultType = rest.getDefaultValue().orElse(null);
            boolean isSerializable = VariableType.OPTIONAL.isSerializable() && rest.isSerializable();
            return new VariableType(isSerializable, defaultType);
        } else if (type instanceof UnionTypeDescriptorNode) {
            // Union cannot be inferred directly
            // Union default cannot be determined. We need internal types for that.
            //  Serializable if all types serializable.
            //  Default value of any type with a default.
            VariableType leftType = fromDescriptor(((UnionTypeDescriptorNode) type).leftTypeDesc());
            VariableType rightType = fromDescriptor(((UnionTypeDescriptorNode) type).rightTypeDesc());
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
            VariableType paramType = fromToken(paramTypeDes.parameterizedType());
            VariableType typeParam = fromDescriptor(paramTypeDes.typeParameter().typeNode());
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
    private static VariableType fromToken(Token type) {
        return TYPE_MAP.getOrDefault(type.kind(), VariableType.UNIDENTIFIED);
    }

    public boolean isSerializable() {
        if (warned) {
            PrinterProvider.debug("Checking serializable-ity on a warned type.");
        }
        return isSerializable;
    }

    public Optional<ExpressionNode> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }
}
