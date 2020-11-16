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

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.Objects;
import java.util.Optional;

/**
 *
 */
public class VariableType {
    // () value
    private static final ExpressionNode NIL_LITERAL_NODE = NodeFactory.createNilLiteralNode(
            NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
            NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN));
    // false value
    private static final ExpressionNode FALSE_LITERAL_NODE = NodeFactory.createBasicLiteralNode(
            SyntaxKind.BOOLEAN_LITERAL, NodeFactory.createToken(SyntaxKind.FALSE_KEYWORD));
    // 0 value
    private static final ExpressionNode ZERO_LITERAL_NODE = NodeFactory.createBasicLiteralNode(
            SyntaxKind.NUMERIC_LITERAL, NodeFactory.createLiteralValueToken(
                    SyntaxKind.DECIMAL_INTEGER_LITERAL_TOKEN, "0", NodeFactory.createEmptyMinutiaeList(),
                    NodeFactory.createEmptyMinutiaeList()));
    // "" value
    private static final ExpressionNode EMPTY_STRING_LITERAL_NODE = NodeFactory.createBasicLiteralNode(
            SyntaxKind.STRING_LITERAL, NodeFactory.createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN, "\"\"",
                    NodeFactory.createEmptyMinutiaeList(), NodeFactory.createEmptyMinutiaeList()));
    // xml`` value
    private static final ExpressionNode EMPTY_XML_NODE = NodeFactory.createTemplateExpressionNode(
            SyntaxKind.XML_TEMPLATE_EXPRESSION, NodeFactory.createToken(SyntaxKind.XML_KEYWORD),
            NodeFactory.createToken(SyntaxKind.BACKTICK_TOKEN), NodeFactory.createNodeList(),
            NodeFactory.createToken(SyntaxKind.BACKTICK_TOKEN));
    // [] value
    private static final ExpressionNode EMPTY_LIST_NODE = NodeFactory.createListConstructorExpressionNode(
            NodeFactory.createToken(SyntaxKind.OPEN_BRACKET_TOKEN), NodeFactory.createSeparatedNodeList(),
            NodeFactory.createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
    // {} value
    private static final ExpressionNode EMPTY_MAP_NODE = NodeFactory.createMappingConstructorExpressionNode(
            NodeFactory.createToken(SyntaxKind.OPEN_BRACE_TOKEN), NodeFactory.createSeparatedNodeList(),
            NodeFactory.createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
    // table[] value
    private static final ExpressionNode EMPTY_TABLE_NODE = NodeFactory.createTableConstructorExpressionNode(
            NodeFactory.createToken(SyntaxKind.TABLE_KEYWORD), null,
            NodeFactory.createToken(SyntaxKind.OPEN_BRACKET_TOKEN), NodeFactory.createSeparatedNodeList(),
            NodeFactory.createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));


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

    private final boolean isSerializable;
    private final ExpressionNode defaultValue;
    private final boolean warned;

    public VariableType(boolean isSerializable) {
        this(isSerializable, null, false);
    }

    public VariableType(boolean isSerializable, ExpressionNode defaultValue) {
        this(isSerializable, defaultValue, false);
        Objects.requireNonNull(defaultValue);
    }

    public VariableType(boolean isSerializable, ExpressionNode defaultValue, boolean warned) {
        this.isSerializable = isSerializable;
        this.defaultValue = defaultValue;
        this.warned = warned;
    }

    public boolean isSerializable() {
        return isSerializable;
    }

    public Optional<ExpressionNode> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public boolean isWarned() {
        return warned;
    }
}
