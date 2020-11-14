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
import io.ballerina.shell.snippet.SnippetKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mustache context for {@code ReEvalExecutor}.
 * The methods in this context would be consumed by the template file.
 */
public class ReEvalContext implements Context {
    private static final String DEFAULT_EXPR = "__NoExpressionError__(\"No expression\")";
    private static final String EXPR_DECLARATION = "__reserved__ = %s;";

    private final List<String> imports;
    private final List<String> moduleDeclarations;
    private final List<String> variableDeclarations;
    private final List<String> statementsAndExpressions;
    private final String newExpression;
    private final String newStatement;

    public ReEvalContext(List<String> imports,
                         List<String> moduleDeclarations,
                         List<String> variableDeclarations,
                         List<String> statementsAndExpressions,
                         String newExpression, String newStatement) {
        this.imports = imports;
        this.moduleDeclarations = moduleDeclarations;
        this.variableDeclarations = variableDeclarations;
        this.statementsAndExpressions = statementsAndExpressions;
        this.newExpression = newExpression;
        this.newStatement = newStatement;
    }

    private static List<String> snippetsToStrings(List<Snippet> snippets) {
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
    public List<String> statementsAndExpressions() {
        return statementsAndExpressions;
    }

    @SuppressWarnings("unused")
    public String newExpression() {
        return Objects.requireNonNullElse(newExpression, DEFAULT_EXPR);
    }

    @SuppressWarnings("unused")
    public String newStatement() {
        return Objects.requireNonNullElse(newStatement, "");
    }


    /**
     * Context creation utility function.
     *
     * @param importSnippets                  Imports.
     * @param moduleDeclarationSnippets       Module level declarations.
     * @param variableDeclarationSnippets     Variable declarations.
     * @param statementsAndExpressionSnippets Statement and expression snippets. (In order)
     * @param newLine                         Newly added snippet.
     * @return Created context.
     */
    public static ReEvalContext create(List<Snippet> importSnippets,
                                       List<Snippet> moduleDeclarationSnippets,
                                       List<Snippet> variableDeclarationSnippets,
                                       List<Snippet> statementsAndExpressionSnippets,
                                       Snippet newLine) {
        List<String> imports = snippetsToStrings(importSnippets);
        List<String> moduleDeclarations = snippetsToStrings(moduleDeclarationSnippets);
        List<String> variableDeclarations = snippetsToStrings(variableDeclarationSnippets);
        List<String> statementsAndExpressions = new ArrayList<>();

        // Get the new statement/new expression - if any
        String newExpression = null;
        String newStatement = null;
        if (newLine != null) {
            if (newLine.getKind() == SnippetKind.EXPRESSION_KIND) {
                newExpression = newLine.toSourceCode();
            } else if (newLine.getKind() == SnippetKind.STATEMENT_KIND) {
                newStatement = newLine.toSourceCode();
            }
        }

        // Reformat expressions
        for (Snippet snippet : statementsAndExpressionSnippets) {
            if (snippet != newLine) {
                String code = snippet.toSourceCode();
                if (snippet.getKind() == SnippetKind.EXPRESSION_KIND) {
                    code = String.format(EXPR_DECLARATION, code);
                }
                statementsAndExpressions.add(code);
            }
        }

        // Remove repeated lines
        statementsAndExpressions.remove(newExpression);
        statementsAndExpressions.remove(newStatement);

        return new ReEvalContext(imports, moduleDeclarations,
                variableDeclarations, statementsAndExpressions,
                newExpression, newStatement);
    }
}

