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

import io.ballerina.compiler.syntax.tree.Node;

import java.util.Objects;

/**
 * Every snippet must have a kind (which dictates where the snippet should go and
 * the sub kind depicting the statement type) Each input line in the REPL can refer to one or more snippets.
 * (Separated by semicolons) These will be handled differently.
 * That means even if the same input line contained several statements,
 * it would be taken as if they were separate lines.
 * <p>
 * In processing the snippets, if a snippet contained an error and failed to run,
 * it would be ditched. Which means that an error snippet is taken as never given.
 * Also, names given to the REPL may never be overridden.
 * (If `x` variable is defined, you cannot redefine variable `x` even with the same type.
 * Same goes for functions, classes etc..)
 *
 * @param <T> Type of the node that corresponds to the snippet.
 */
public abstract class Snippet<T extends Node> {
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

    protected final T node;
    protected final SnippetKind kind;

    public Snippet(T node, SnippetKind kind) {
        this.node = Objects.requireNonNull(node);
        this.kind = kind;
    }

    /**
     * Category of the snippet.
     *
     * @return kind of the snippet.
     */
    public SnippetKind getKind() {
        return kind;
    }

    /**
     * Converts the snippet into source code.
     *
     * @return Source code corresponding to snippet.
     */
    public String toSourceCode() {
        return node.toSourceCode();
    }

    /**
     * Persisted property associated with the snippet kind.
     *
     * @return Whether the snippet is persisted.
     */
    public boolean isPersistent() {
        return kind.isPersistent();
    }
}
