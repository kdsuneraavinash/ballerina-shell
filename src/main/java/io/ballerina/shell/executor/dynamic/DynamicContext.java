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

package io.ballerina.shell.executor.dynamic;

import io.ballerina.shell.executor.Context;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.util.List;
import java.util.Objects;

/**
 * Mustache context for {@link DynamicExecutor}.
 * The methods in this context would be consumed by the template file.
 */
public class DynamicContext extends Context {
    private static final String DEFAULT_EXPR = "__NoExpressionError__(\"No expression\")";

    private final List<String> imports;
    private final List<String> moduleDeclarations;
    private final List<String> variableDefinitions;

    public DynamicContext(List<String> imports,
                          List<String> moduleDeclarations,
                          List<String> variableDefinitions,
                          Snippet newSnippet) {
        super(newSnippet);
        this.imports = imports;
        this.moduleDeclarations = moduleDeclarations;
        this.variableDefinitions = variableDefinitions;
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
     * @param state      State object to populate context.
     * @param newSnippet Newly added snippet.
     * @return Created context.
     */
    public static DynamicContext create(DynamicState state, Snippet newSnippet) {
        List<String> imports = snippetsToStrings(state.imports());
        List<String> moduleDeclarations = snippetsToStrings(state.moduleDeclarations());
        List<String> variableDefinitions = snippetsToStrings(state.variableDefinitions());

        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            imports.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DEFINITION_KIND) {
            variableDefinitions.add(newSnippet.toSourceCode());
        }

        return new DynamicContext(imports, moduleDeclarations, variableDefinitions, newSnippet);
    }
}
