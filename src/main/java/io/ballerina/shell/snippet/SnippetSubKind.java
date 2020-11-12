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

/**
 * Subkind further narrows down a snippet kind.
 * Each subkind as a fixed parent kind.
 */
public enum SnippetSubKind {
    IMPORT("You cannot import external modules while in REPL"),

    VARIABLE_DEFINITION(SnippetKind.VARIABLE_DEFINITION_KIND),
    VARIABLE_DEFINITION_WITHOUT_VALUE("Variable definitions in REPL must initialize with a value"), // Error

    FUNCTION_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    LISTENER_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    TYPE_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    SERVICE_DECLARATION("Services cannot be declared in REPL"), // Error
    CONSTANT_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    MODULE_VARIABLE_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND, true), // Ignored
    ANNOTATION_DECLARATION("Defining annotations in REPL are not currently supported"), // Error
    MODULE_XML_NAMESPACE_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    ENUM_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),
    CLASS_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION_KIND),

    ASSIGNMENT_STATEMENT_SUBKIND(SnippetKind.STATEMENT_KIND),
    COMPOUND_ASSIGNMENT_STATEMENT_SUBKIND(SnippetKind.STATEMENT_KIND),
    VARIABLE_DECLARATION_STATEMENT(SnippetKind.STATEMENT_KIND, true), // Ignored
    BLOCK_STATEMENT(SnippetKind.STATEMENT_KIND),
    BREAK_STATEMENT("Break cannot be used outside of a loop."), // Error
    FAIL_STATEMENT("Fail statements must appear inside a function."), // Error
    EXPRESSION_STATEMENT(SnippetKind.STATEMENT_KIND, true), // Ignored
    CONTINUE_STATEMENT("Continue cannot be used outside of a loop."), // Error
    IF_ELSE_STATEMENT(SnippetKind.STATEMENT_KIND),
    WHILE_STATEMENT(SnippetKind.STATEMENT_KIND),
    PANIC_STATEMENT(SnippetKind.STATEMENT_KIND),
    RETURN_STATEMENT("Return cannot exist outside of a function."), // Error
    LOCAL_TYPE_DEFINITION_STATEMENT(SnippetKind.STATEMENT_KIND, true), // Ignored
    LOCK_STATEMENT(SnippetKind.STATEMENT_KIND),
    FORK_STATEMENT(SnippetKind.STATEMENT_KIND),
    FOR_EACH_STATEMENT(SnippetKind.STATEMENT_KIND),
    XML_NAMESPACE_DECLARATION_STATEMENT(SnippetKind.STATEMENT_KIND, true), // Ignored
    TRANSACTION_STATEMENT(SnippetKind.STATEMENT_KIND),
    ROLLBACK_STATEMENT("Rollback cannot be used outside of a transaction block."), // Error
    RETRY_STATEMENT(SnippetKind.STATEMENT_KIND),
    MATCH_STATEMENT(SnippetKind.STATEMENT_KIND),
    DO_STATEMENT(SnippetKind.STATEMENT_KIND),

    TYPE_TEST_EXPRESSION(SnippetKind.EXPRESSION_KIND), // Issue
    TABLE_CONSTRUCTOR_EXPRESSION("Table expressions are disallowed"), // Issue
    SERVICE_CONSTRUCTOR_EXPRESSION("Service constructors are disallowed"), // Issue
    OTHER_EXPRESSION(SnippetKind.EXPRESSION_KIND),

    ERROR("Unidentified statement? "); // Error

    private final SnippetKind kind;
    private final boolean ignored;
    private final String errorMessage;

    SnippetSubKind(SnippetKind kind, boolean ignored) {
        this.kind = kind;
        this.ignored = ignored;
        this.errorMessage = null;
    }

    SnippetSubKind(String errorMessage) {
        this.kind = SnippetKind.ERRONEOUS_KIND;
        this.ignored = true;
        this.errorMessage = errorMessage;
    }

    SnippetSubKind(SnippetKind kind) {
        this(kind, false);
    }

    /**
     * Ignored sub kinds are sub kinds that are handled by
     * another kind/sub kind. As a result they do not need handling as
     * this particular sub-kind.
     *
     * @return Whether the sub kind is ignored.
     */
    public boolean isIgnored() {
        return ignored;
    }

    /**
     * Kind of a subkind is the parent base type
     * of the snippet. Kind determines mainly the location
     * of the snippet in a AST.
     *
     * @return Parent kind of the snippet sub kind.
     */
    public SnippetKind getKind() {
        return kind;
    }

    /**
     * Subkind has an error message if the kind is a {@code ERRONEOUS_KIND}.
     *
     * @return Error message.
     */
    public String getErrorMessage() {
        assert kind == SnippetKind.ERRONEOUS_KIND;
        return errorMessage;
    }
}
