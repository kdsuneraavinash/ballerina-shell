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

package io.ballerina.shell.executor.reeval;

import io.ballerina.shell.executor.Context;
import io.ballerina.shell.snippet.Snippet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mustache context for {@code ReEvalExecutor}.
 * The methods in this context would be consumed by the template file.
 */
public class ReEvalContext implements Context {
    private static final String DEFAULT_EXPR = "\"\\u{001b}[30;1mOK\\u{001b}[0m\"";

    private final List<String> imports;
    private final List<String> moduleDeclarations;
    private final List<String> variableDeclarations;
    private final List<String> statements;
    private final List<String> expressions;
    private final String newExpression;

    public ReEvalContext(
            List<Snippet> imports,
            List<Snippet> moduleDeclarations,
            List<Snippet> variableDeclarations,
            List<Snippet> statements,
            List<Snippet> expressions,
            Snippet newExpression) {
        this.imports = snippetsToStrings(imports);
        this.moduleDeclarations = snippetsToStrings(moduleDeclarations);
        this.variableDeclarations = snippetsToStrings(variableDeclarations);
        this.statements = snippetsToStrings(statements);
        this.expressions = snippetsToStrings(expressions);
        this.newExpression = (newExpression == null) ? null : newExpression.toSourceCode();
        this.expressions.remove(this.newExpression);
    }

    private List<String> snippetsToStrings(List<Snippet> snippets) {
        List<String> strings = new ArrayList<>();
        for (Snippet snippet : snippets) {
            strings.add(snippet.toSourceCode());
        }
        return strings;
    }

    @SuppressWarnings("unused")
    public List<String> imports() {
        return imports;
    }

    @SuppressWarnings("unused")
    public List<String> moduleDeclarations() {
        return moduleDeclarations;
    }

    @SuppressWarnings("unused")
    public List<String> variableDeclarations() {
        return variableDeclarations;
    }

    @SuppressWarnings("unused")
    public List<String> statements() {
        return statements;
    }

    @SuppressWarnings("unused")
    public List<String> expressions() {
        return expressions;
    }

    @SuppressWarnings("unused")
    public String newExpression() {
        return Objects.requireNonNullElse(newExpression, DEFAULT_EXPR);
    }
}
