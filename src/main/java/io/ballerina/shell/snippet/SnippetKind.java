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
 * Kind determines the subtype of the snippet.
 * This can be used to cast to the subtype.
 */
public enum SnippetKind {
    IMPORT_KIND(true),
    MODULE_MEMBER_DECLARATION_KIND(true),
    VARIABLE_DEFINITION_KIND(true),
    STATEMENT_KIND(true),
    EXPRESSION_KIND(false),
    ERRONEOUS_KIND(false);

    private final boolean isPersistent;

    SnippetKind(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    /**
     * Whether the kind is a persistent type.
     * Persistent snippets remain in memory even after their execution.
     *
     * @return Whether the snippet is persistent..
     */
    public boolean isPersistent() {
        return isPersistent;
    }
}
