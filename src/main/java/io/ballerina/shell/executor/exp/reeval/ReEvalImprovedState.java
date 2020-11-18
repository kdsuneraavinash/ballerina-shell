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

package io.ballerina.shell.executor.exp.reeval;

import com.google.gson.Gson;
import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.executor.State;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State of the {@link ReEvalImprovedExecutor}.
 */
public class ReEvalImprovedState implements State {
    private static final String STATE_DUMP = "state.dump";

    /**
     * Serialized subset of the state.
     * This is used as a type def to load the state data from dump.
     */
    private static class SerializedState {
        private final Map<String, String> vars;
        private final boolean isStmtPreserved;

        private SerializedState(Map<String, String> vars, boolean isStmtPreserved) {
            this.vars = vars;
            this.isStmtPreserved = isStmtPreserved;
        }

        @Override
        public String toString() {
            return String.format("State[var: %s, isStmtPreserved: %s]", vars.keySet(), isStmtPreserved);
        }
    }

    private final List<Snippet> imports;
    private final List<Snippet> moduleDeclarations;
    private final Map<String, VariableDeclarationSnippet> variableDefinitions;
    private final List<Snippet> statementsAndExpressions;
    private final Gson gson;

    public ReEvalImprovedState() {
        imports = new ArrayList<>();
        moduleDeclarations = new ArrayList<>();
        variableDefinitions = new HashMap<>();
        statementsAndExpressions = new ArrayList<>();
        gson = new Gson();
    }

    @Override
    public void reset() {
        PrinterProvider.debug("Resetting ReEval State.");
        imports.clear();
        moduleDeclarations.clear();
        variableDefinitions.clear();
        statementsAndExpressions.clear();
    }

    /**
     * Adds a snippet to state on the correct array depending on the
     * type of new snippet.
     * For example, if the new snippet is a expression, that list would be operated.
     *
     * @param newSnippet Snippet to check the type of.
     */
    public void saveState(Snippet newSnippet) throws ExecutorException {
        // === Load state ===
        // 1. Read the dump file and put all the variables found
        //    into the state. (The variable current value will be put as the initializer)
        // 2. Add the new snippet into corresponding category.
        // 3. If a new variable was declared and it has changed the state
        //    then new variable was not serialized. Then issue a warning and save it as well.
        // 4. Otherwise, if the new snippet was a statement or expression and state was not serialized,
        //    add it to state. If the state was serialized, ditch the statement/expression.

        SerializedState serializedState;

        try (FileReader fr = new FileReader(STATE_DUMP, Charset.defaultCharset())) {
            serializedState = gson.fromJson(fr, SerializedState.class);
        } catch (IOException e) {
            throw new ExecutorException("State load/save failed." + e.getMessage());
        }

        PrinterProvider.debug(serializedState.toString());
        for (String name : serializedState.vars.keySet()) {
            String initializer = serializedState.vars.get(name);
            if (variableDefinitions.containsKey(name)) {
                // Previously defined variable - re-add with this value as initializer
                variableDefinitions.put(name, variableDefinitions.get(name).withInitializer(initializer));
            }
        }

        if (newSnippet.isImport()) {
            // imports dont do anything, no state change
            imports.add(newSnippet);
        } else if (newSnippet.isModuleMemberDeclaration()) {
            // Module level declarations dont do anything, no state change
            moduleDeclarations.add(newSnippet);
        } else if (newSnippet.isVariableDeclaration()) {
            assert newSnippet instanceof VariableDeclarationSnippet;
            VariableDeclarationSnippet varSnippet = (VariableDeclarationSnippet) newSnippet;
            if (serializedState.isStmtPreserved) {
                // New variable - state must be in serialized
                variableDefinitions.put(varSnippet.getVariableName(),
                        varSnippet.withInitializer(serializedState.vars.get(varSnippet.getVariableName())));
            } else {
                PrinterProvider.warn("" +
                        "A non-simple variable " + varSnippet.getVariableName() + " declared.\n" +
                        "Any changes to this variable will be re-evaluated in each expression.\n" +
                        "So do not store any random/external data in this variable.");
                // A non-serializable variable declared, need to add the raw snippet
                variableDefinitions.put(varSnippet.getVariableName(), varSnippet);
            }
        } else if (newSnippet.isStatement() || newSnippet.isExpression()) {
            if (!serializedState.isStmtPreserved) {
                // Need to add the variable states just before this execution
                // Since this is a new statement, no var dcln
                statementsAndExpressions.add(newSnippet);
            }
        }

        PrinterProvider.debug(this.toString());
    }

    public List<Snippet> imports() {
        return imports;
    }

    public List<Snippet> moduleDeclarations() {
        return moduleDeclarations;
    }

    public List<Snippet> variableDeclarations() {
        return new ArrayList<>(variableDefinitions.values());
    }

    public List<Snippet> statementsAndExpressions() {
        return statementsAndExpressions;
    }

    public Map<String, VariableDeclarationSnippet> variableDefinitions() {
        return variableDefinitions;
    }

    @Override
    public String toString() {
        return String.format("[imports: %s, moduleDcln: %s, varDefs: %s, stmtsExprs: %s]",
                imports.size(), moduleDeclarations.size(), variableDefinitions.size(), statementsAndExpressions.size());
    }
}
