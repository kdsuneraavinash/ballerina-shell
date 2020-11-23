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
 * Snippet sub type to further categorize snippets.
 */
public enum SnippetSubKind {
    IMPORT_DECLARATION(SnippetKind.IMPORT_DECLARATION, true, false),

    VARIABLE_DECLARATION(SnippetKind.VARIABLE_DECLARATION, true, false),

    // Module member declarations - none is executable
    FUNCTION_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    LISTENER_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    TYPE_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    SERVICE_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, "Services are not allowed within REPL."), // Error
    CONSTANT_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    MODULE_VARIABLE_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true), // Ignore
    ANNOTATION_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    MODULE_XML_NAMESPACE_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    ENUM_DECLARATION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),
    CLASS_DEFINITION(SnippetKind.MODULE_MEMBER_DECLARATION, true, false),

    // Statements - everything is executable
    ASSIGNMENT_STATEMENT(SnippetKind.STATEMENT, true, true),
    COMPOUND_ASSIGNMENT_STATEMENT(SnippetKind.STATEMENT, true, true),
    VARIABLE_DECLARATION_STATEMENT(SnippetKind.STATEMENT, true), // Ignore
    BLOCK_STATEMENT(SnippetKind.STATEMENT, true, true),
    BREAK_STATEMENT(SnippetKind.STATEMENT, "Break cannot be used outside of a loop."), // Error
    FAIL_STATEMENT(SnippetKind.STATEMENT, "Fail statements must appear inside a function."), // Error
    EXPRESSION_STATEMENT(SnippetKind.STATEMENT, true), // Ignore
    CONTINUE_STATEMENT(SnippetKind.STATEMENT, "Continue cannot be used outside of a loop."), // Error
    IF_ELSE_STATEMENT(SnippetKind.STATEMENT, true, true),
    WHILE_STATEMENT(SnippetKind.STATEMENT, true, true),
    PANIC_STATEMENT(SnippetKind.STATEMENT, true, true),
    RETURN_STATEMENT(SnippetKind.STATEMENT, "Return cannot exist outside of a function."), // Error
    LOCAL_TYPE_DEFINITION_STATEMENT(SnippetKind.STATEMENT, true), // Ignore
    LOCK_STATEMENT(SnippetKind.STATEMENT, true, true),
    FORK_STATEMENT(SnippetKind.STATEMENT, true, true),
    FOR_EACH_STATEMENT(SnippetKind.STATEMENT, true, true),
    XML_NAMESPACE_DECLARATION_STATEMENT(SnippetKind.STATEMENT, true), // Ignore
    TRANSACTION_STATEMENT(SnippetKind.STATEMENT, true, true),
    ROLLBACK_STATEMENT(SnippetKind.STATEMENT, "Rollback cannot be used outside of a transaction block."), // Error
    RETRY_STATEMENT(SnippetKind.STATEMENT, true, true),
    MATCH_STATEMENT(SnippetKind.STATEMENT, true, true),
    DO_STATEMENT(SnippetKind.STATEMENT, true, true),

    EXPRESSION(SnippetKind.EXPRESSION, true, true);

    private final SnippetKind kind;
    private final boolean isPersistent;
    private final boolean isExecutable;
    private final boolean isIgnored;
    private final String error;

    SnippetSubKind(SnippetKind kind, boolean isPersistent, boolean isExecutable) {
        this.kind = kind;
        this.isPersistent = isPersistent;
        this.isExecutable = isExecutable;
        this.isIgnored = false;
        this.error = null;
    }

    SnippetSubKind(SnippetKind kind, boolean isIgnored) {
        this.kind = kind;
        this.isPersistent = false;
        this.isExecutable = false;
        this.isIgnored = isIgnored;
        this.error = null;
    }

    SnippetSubKind(SnippetKind kind, String error) {
        this.kind = kind;
        this.isPersistent = false;
        this.isExecutable = false;
        this.isIgnored = false;
        this.error = error;
    }

    public SnippetKind getKind() {
        return kind;
    }

    public boolean isExecutable() {
        return isExecutable;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public boolean hasError() {
        return error != null;
    }

    public String getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public boolean isPersistent() {
        return isPersistent;
    }
}
