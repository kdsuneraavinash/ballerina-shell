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
import io.ballerina.shell.executor.Context;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.invoker.AsyncShellInvoker;
import io.ballerina.shell.executor.invoker.ShellInvoker;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.types.ImportSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Executes the snippet given.
 * Re evaluates the snippet by generating a file containing all snippets
 * and executing it.
 */
public class ReEvalExecutor extends Executor<ReEvalState, ReEvalContext, ShellInvoker> {
    private static final String TEMPLATE_FILE = "template.reeval.ftl";
    private static final String GENERATED_FILE = "main.bal";

    public ReEvalExecutor() {
        this(TEMPLATE_FILE, new ReEvalState(), new AsyncShellInvoker(GENERATED_FILE));
    }

    public ReEvalExecutor(String templateName, ReEvalState state, ShellInvoker invoker) {
        super(templateName, state, invoker);
    }

    @Override
    public ReEvalContext currentContext(Snippet newSnippet) {
        List<String> imports = Context.snippetsToStrings(state.imports());
        List<String> moduleDeclarations = Context.snippetsToStrings(state.moduleDeclarations());
        List<String> variableDeclarations = Context.snippetsToStrings(state.variableDeclarations());
        List<ReEvalContext.StatementExpression> statementsAndExpressions = new ArrayList<>();
        ReEvalContext.StatementExpression newStatementOrExpression = new ReEvalContext.StatementExpression(newSnippet);
        Map<String, VariableDeclarationSnippet> variables = new HashMap<>(state.variableDefinitions());
        String newVarName = "_undefined";

        Set<String> serializableVariableNames = new HashSet<>();
        Set<String> allVariableNames = new HashSet<>();

        for (String name : variables.keySet()) {
            allVariableNames.add(name);
            if (variables.get(name).isSerializable()) {
                serializableVariableNames.add(name);
            }
        }

        // Add to correct category
        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            assert newSnippet instanceof ImportSnippet;
            imports.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DECLARATION_KIND) {
            variableDeclarations.add(newSnippet.toSourceCode());
            // Add to variable names for context
            // TODO: What if variable name is null
            assert newSnippet instanceof VariableDeclarationSnippet;
            VariableDeclarationSnippet varSnippet = (VariableDeclarationSnippet) newSnippet;
            Objects.requireNonNull(varSnippet.getVariableName(), "Invalid variable declaration.");
            newVarName = varSnippet.getVariableName();
            allVariableNames.add(newVarName);
            if (varSnippet.isSerializable()) {
                serializableVariableNames.add(newVarName);
            }
        }

        // Reformat expressions
        for (Snippet snippet : state.statementsAndExpressions()) {
            if (snippet != newSnippet) {
                statementsAndExpressions.add(new ReEvalContext.StatementExpression(snippet));
            }
        }

        // Clear previous file
        try (FileWriter fw = new FileWriter("state.dump", Charset.defaultCharset())) {
            fw.write("");
            PrinterProvider.debug("Previous state file cleared.");
        } catch (IOException ignored) {
        }

        return new ReEvalContext(imports, moduleDeclarations,
                variableDeclarations, statementsAndExpressions, newStatementOrExpression,
                allVariableNames, serializableVariableNames, newVarName);
    }

    @Override
    public boolean executeInvoker(Postprocessor postprocessor) throws IOException, InterruptedException {
        return invoker.execute(postprocessor);
    }

    @Override
    public void onSuccess(Snippet newSnippet) {
        // Add snippet to state
        state.saveState(newSnippet);
    }
}
