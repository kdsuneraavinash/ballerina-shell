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

import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.types.ExpressionSnippet;
import io.ballerina.shell.snippet.types.ImportSnippet;
import io.ballerina.shell.snippet.types.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.types.StatementSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;

/**
 * Snippets are individual statements.
 * <p>
 * Every snippet must have a kind (which dictates where the snippet should go) and a
 * sub kind (depicting the statement type) Each snippet must refer to a single statement.
 * That means if the same input line contained several statements, it would be parsed
 * into several snippets. (This separation is done in preprocessor.)
 * <p>
 * In processing the snippets, if a snippet contained an error and failed to run,
 * the execution of the snippet would be stopped. If the snippet was contained in a
 * line with more snippets, (if the input contained multiple snippets)
 * all the snippets would be ditched.
 * This also means that an error snippet is taken as if it were never given.
 * <p>
 * Also, names given to the REPL may never be overridden.
 * (If {@code x} variable is defined, you cannot redefine variable {@code x} even
 * with the same type. Same goes for functions, classes etc..)
 * However, any valid redeclaration in a different scope may be possible.
 */
public abstract class Snippet {
    protected final SnippetSubKind subKind;
    protected final String sourceCode;

    protected Snippet(String sourceCode, SnippetSubKind subKind) {
        this.sourceCode = sourceCode;
        this.subKind = subKind;
    }

    /**
     * Converts the snippet into source code.
     *
     * @return Source code corresponding to snippet.
     */
    public String toSourceCode() {
        return sourceCode;
    }

    /**
     * Ignored snippets are snippets that are handled by
     * another snippet type. As a result they do not need handling as
     * this particular snippet.
     *
     * @return Whether the snippet is ignored.
     */
    public boolean isIgnored() {
        return subKind.isIgnored();
    }

    /**
     * Throws an exception if the snippet has some error.
     *
     * @throws SnippetException If snippet has an error.
     */
    public void throwIfSnippetHasError() throws SnippetException {
        if (subKind.getKind() == SnippetKind.ERRONEOUS_KIND) {
            throw new SnippetException(subKind.getErrorMessage());
        }
    }

    public boolean isImport() {
        return this instanceof ImportSnippet;
    }

    public boolean isModuleMemberDeclaration() {
        return this instanceof ModuleMemberDeclarationSnippet;
    }

    public boolean isStatement() {
        return this instanceof StatementSnippet;
    }

    public boolean isExpression() {
        return this instanceof ExpressionSnippet;
    }

    public boolean isVariableDeclaration() {
        return this instanceof VariableDeclarationSnippet;
    }

    @Override
    public String toString() {
        return sourceCode;
    }
}
