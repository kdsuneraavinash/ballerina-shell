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

import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.executor.State;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * State of the {@link ReEvalExecutor}.
 */
public class ReEvalState implements State {
    private final List<Snippet> imports;
    private final List<Snippet> moduleDeclarations;
    private final List<Snippet> variableDefinitions;
    private final List<Snippet> statementsAndExpressions;
    private final Set<String> variableNames;

    public ReEvalState() {
        imports = new ArrayList<>();
        moduleDeclarations = new ArrayList<>();
        variableDefinitions = new ArrayList<>();
        statementsAndExpressions = new ArrayList<>();
        variableNames = new HashSet<>();
    }

    @Override
    public void reset() {
        PrinterProvider.debug("Resetting ReEval State.");
        imports.clear();
        moduleDeclarations.clear();
        variableDefinitions.clear();
        statementsAndExpressions.clear();
        variableNames.clear();
    }

    /**
     * Adds a snippet to state on the correct array depending on the
     * type of new snippet.
     * For example, if the new snippet is a expression, that list would be operated.
     *
     * @param newSnippet Snippet to check the type of.
     */
    public void addSnippet(Snippet newSnippet) {
        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            imports.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DECLARATION_KIND) {
            variableDefinitions.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.STATEMENT_KIND) {
            statementsAndExpressions.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.EXPRESSION_KIND) {
            statementsAndExpressions.add(newSnippet);
        }
    }

    public void addNewVariableName(String name) {
        variableNames.add(name);
    }

    public List<Snippet> imports() {
        return imports;
    }

    public List<Snippet> moduleDeclarations() {
        return moduleDeclarations;
    }

    public List<Snippet> variableDeclarations() {
        return variableDefinitions;
    }

    public List<Snippet> statementsAndExpressions() {
        return statementsAndExpressions;
    }

    public Set<String> variableNames() {
        return variableNames;
    }
}
