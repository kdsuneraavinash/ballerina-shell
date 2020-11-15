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

import java.util.List;
import java.util.Objects;

/**
 * Mustache context for {@link ReEvalExecutor}.
 * The methods in this context would be consumed by the template file.
 */
public class ReEvalContext extends Context {
    private static final String DEFAULT_EXPR = "__NoExpressionError__(\"No expression\")";
    private static final String EXPR_DECLARATION = "__reserved__ = %s;";

    private final List<String> imports;
    private final List<String> moduleDeclarations;
    private final List<String> variableDefinitions;
    private final List<String> statementsAndExpressions;

    public ReEvalContext(List<String> imports,
                         List<String> moduleDeclarations,
                         List<String> variableDefinitions,
                         List<String> statementsAndExpressions,
                         Snippet newSnippet) {
        super(newSnippet);
        this.imports = imports;
        this.moduleDeclarations = moduleDeclarations;
        this.variableDefinitions = variableDefinitions;
        this.statementsAndExpressions = statementsAndExpressions;

        // Remove repeated lines
        statementsAndExpressions.remove(newExpression);
        statementsAndExpressions.remove(newStatement);
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
    public List<String> variableDefinitions() {
        return variableDefinitions;
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
     * Formats expression to be put on template.
     * Expressions cannot be directly put as a statement.
     * So they have to be formatted as a variable assignment.
     *
     * @param expression Expression to format.
     * @return Created context.
     */
    public static String formatExpression(String expression) {
        return String.format(EXPR_DECLARATION, expression);
    }
}

