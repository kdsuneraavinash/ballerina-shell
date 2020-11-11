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
    protected final T node;
    protected final SnippetSubKind subKind;

    protected Snippet(T node, SnippetSubKind subKind) {
        this.node = Objects.requireNonNull(node);
        this.subKind = subKind;
    }

    /**
     * Category of the snippet.
     *
     * @return kind of the snippet.
     */
    public SnippetKind getKind() {
        return subKind.getKind();
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
        return getKind().isPersistent();
    }

    /**
     * Ignored snippets are snippets that are handled by
     * another kind.
     *
     * @return Whether the sub kind is ignored.
     */
    public boolean isIgnored() {
        return subKind.isIgnored();
    }

    /**
     * Executable snippets must be evaluated as soon as possible.
     * If a snippet is not executable, its execution can be deferred.
     *
     * @return Whether the snippet is executable.
     */
    public boolean isExecutable() {
        return subKind.isExecutable();
    }

    /**
     * Valid snippets are all the snippets that are not erroneous and not ignored.
     *
     * @return Whether the snippet is valid.
     */
    public boolean isValid() {
        if (getKind() == SnippetKind.ERRONEOUS_KIND) {
            throw new RuntimeException(subKind.getErrorMessage());
        }
        return !isIgnored();
    }
}
