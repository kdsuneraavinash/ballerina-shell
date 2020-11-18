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
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.utils.NodeUtilities;

import java.util.Objects;
import java.util.Optional;

/**
 * Variable type describes the type of a variable.
 * This also defines whether the type is serializable or not.
 */
public class VariableType {
    private static final ExpressionNode NO_DEFAULT = null;
    private static final String NIL_LITERAL_NODE = "()";
    private static final String FALSE_LITERAL_NODE = "false";
    private static final String ZERO_LITERAL_NODE = "0";
    private static final String EMPTY_STRING_LITERAL_NODE = "\"\"";
    private static final String EMPTY_XML_NODE = "xml `<!---->`";
    private static final String EMPTY_TABLE_NODE = "table []";
    private static final String EMPTY_LIST_NODE = "[]";
    private static final String EMPTY_MAP_NODE = "{}";

    public static final VariableType NIL = new VariableType(true, NIL_LITERAL_NODE); // ()
    public static final VariableType BOOLEAN = new VariableType(true, FALSE_LITERAL_NODE); // boolean
    public static final VariableType INT = new VariableType(true, ZERO_LITERAL_NODE); // int
    public static final VariableType FLOAT = new VariableType(true, ZERO_LITERAL_NODE); // float
    public static final VariableType DECIMAL = new VariableType(true, ZERO_LITERAL_NODE); // decimal
    public static final VariableType STRING = new VariableType(true, EMPTY_STRING_LITERAL_NODE); // string
    public static final VariableType XML = new VariableType(true, EMPTY_XML_NODE); // xml
    public static final VariableType ARRAY = new VariableType(true, EMPTY_LIST_NODE); // array
    public static final VariableType TUPLE = new VariableType(true, EMPTY_LIST_NODE); // tuple
    public static final VariableType MAP = new VariableType(true, EMPTY_MAP_NODE); // map
    public static final VariableType RECORD = new VariableType(false, EMPTY_MAP_NODE); // record
    public static final VariableType TABLE = new VariableType(false, EMPTY_TABLE_NODE); // table
    public static final VariableType ERROR = new VariableType(false, NO_DEFAULT); // table
    public static final VariableType FUNCTION = new VariableType(false, NO_DEFAULT); // function
    public static final VariableType FUTURE = new VariableType(false, NO_DEFAULT); // future
    public static final VariableType OBJECT = new VariableType(false, NO_DEFAULT); // object
    public static final VariableType SERVICE = new VariableType(false, NO_DEFAULT); // service
    public static final VariableType TYPEDESC = new VariableType(false, NO_DEFAULT); // typedesc
    public static final VariableType HANDLE = new VariableType(false, NO_DEFAULT); // handle
    public static final VariableType STREAM = new VariableType(false, NO_DEFAULT); // stream
    public static final VariableType SINGLETON = new VariableType(false, NO_DEFAULT); // singleton
    public static final VariableType UNION = new VariableType(true, NO_DEFAULT); // union
    public static final VariableType OPTIONAL = new VariableType(false, NIL_LITERAL_NODE); // optional
    public static final VariableType ANY = new VariableType(false, NIL_LITERAL_NODE); // any
    public static final VariableType ANYDATA = new VariableType(true, NIL_LITERAL_NODE); // anydata
    public static final VariableType NEVER = new VariableType(false, NO_DEFAULT); // never
    public static final VariableType BYTE = new VariableType(true, ZERO_LITERAL_NODE); // byte
    public static final VariableType JSON = new VariableType(true, NIL_LITERAL_NODE); // json
    public static final VariableType UNIDENTIFIED = new VariableType(false, (ExpressionNode) null); // unknown

    private final boolean isSerializable;
    private final ExpressionNode defaultValue;
    private final String errorMessage;

    /**
     * Constructor to create the type with a code snippet as the default.
     *
     * @param isSerializable Whether the type is serializable.
     * @param defaultValue   Default initializer string representation.
     */
    public VariableType(boolean isSerializable, String defaultValue) {
        this.isSerializable = isSerializable;
        this.defaultValue = NodeUtilities.tryToParseExpression(defaultValue);
        Objects.requireNonNull(this.defaultValue);
        this.errorMessage = null;
    }

    /**
     * Constructor to create the type with a expression node as the default.
     *
     * @param isSerializable Whether the type is serializable.
     * @param defaultValue   Default initializer node.
     */
    public VariableType(boolean isSerializable, ExpressionNode defaultValue) {
        this.isSerializable = isSerializable;
        this.defaultValue = defaultValue;
        this.errorMessage = null;
    }

    /**
     * Constructor for invalid variable types.
     *
     * @param errorMessage Error message for the type.
     */
    @SuppressWarnings("unused")
    private VariableType(String errorMessage) {
        this.isSerializable = false;
        this.defaultValue = null;
        this.errorMessage = errorMessage;
    }

    /**
     * Throw an error if the type is invalid.
     *
     * @throws SnippetException If type is invalid.
     */
    public VariableType validated() throws SnippetException {
        if (errorMessage != null) {
            throw new SnippetException(errorMessage);
        }
        return this;
    }

    public boolean isSerializable() {
        return isSerializable;
    }

    public Optional<ExpressionNode> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }
}
