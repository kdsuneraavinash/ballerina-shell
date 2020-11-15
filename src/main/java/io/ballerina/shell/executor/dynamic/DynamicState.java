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

import io.ballerina.shell.executor.State;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State of the {@link DynamicExecutor}.
 */
public class DynamicState implements State {
    // TODO: Replace with set because order does not matter
    private final List<Snippet> imports;
    private final List<Snippet> moduleDeclarations;
    private final List<Snippet> variableDefinitions;
    private final Map<String, Object> variableStates;


    public DynamicState() {
        imports = new ArrayList<>();
        moduleDeclarations = new ArrayList<>();
        variableDefinitions = new ArrayList<>();
        variableStates = new HashMap<>();
    }

    @Override
    public void reset() {
        imports.clear();
        moduleDeclarations.clear();
        variableDefinitions.clear();
        variableStates.clear();
    }

    @Override
    public void addSnippet(Snippet newSnippet) {
        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            imports.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet);
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DEFINITION_KIND) {
            variableDefinitions.add(newSnippet);
        }
    }

    public Object getVariableState(String name) {
        return variableStates.get(name);
    }

    public void setVariableState(String name, Object value) {
        variableStates.put(name, value);
    }

    public boolean containsVariableState(String name) {
        return variableStates.containsKey(name);
    }

    public Iterable<Snippet> imports() {
        return imports;
    }

    public Iterable<Snippet> moduleDeclarations() {
        return moduleDeclarations;
    }

    public Iterable<Snippet> variableDefinitions() {
        return variableDefinitions;
    }
}
