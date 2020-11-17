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
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.invoker.AsyncShellInvoker;
import io.ballerina.shell.executor.invoker.ShellInvoker;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.types.ImportSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
        HashSet<String> variableNames = new HashSet<>(state.variableNames());

        // Add to correct category
        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            assert newSnippet instanceof ImportSnippet;
            imports.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DECLARATION_KIND) {
            variableDeclarations.add(newSnippet.toSourceCode());
            // Add to variable names for context
            assert newSnippet instanceof VariableDeclarationSnippet;
            VariableDeclarationSnippet varSnippet = (VariableDeclarationSnippet) newSnippet;
            if (varSnippet.isSerializable()) {
                variableNames.add(varSnippet.getVariableName());
            }
        }

        // Reformat expressions
        for (Snippet snippet : state.statementsAndExpressions()) {
            if (snippet != newSnippet) {
                statementsAndExpressions.add(new ReEvalContext.StatementExpression(snippet));
            }
        }

        return new ReEvalContext(imports, moduleDeclarations,
                variableDeclarations, statementsAndExpressions, newStatementOrExpression,
                variableNames);
    }

    @Override
    public boolean executeInvoker(Postprocessor postprocessor) throws IOException, InterruptedException {
        return invoker.execute(postprocessor);
    }

    @Override
    public void onSuccess(Snippet newSnippet) {
        // Add snippet to state
        state.addSnippet(newSnippet);
        // Permanently add name if a var dcln snippet
        if (newSnippet instanceof VariableDeclarationSnippet) {
            VariableDeclarationSnippet varSnippet = (VariableDeclarationSnippet) newSnippet;
            if (varSnippet.isSerializable()) {
                state.addNewVariableName(varSnippet.getVariableName());
                DebugProvider.sendMessage("Current variable names: " + state.variableNames());
            }
        }
    }
}
