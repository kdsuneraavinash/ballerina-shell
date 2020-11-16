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
import io.ballerina.shell.executor.reeval.invoker.ReEvalAsyncInvoker;
import io.ballerina.shell.executor.reeval.invoker.ReEvalInvoker;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.ImportSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes the snippet given.
 * Re evaluates the snippet by generating a file containing all snippets
 * and executing it.
 */
public class ReEvalExecutor extends Executor<ReEvalState, ReEvalContext, ReEvalInvoker> {
    private static final String TEMPLATE_FILE = "template.reeval.ftl";
    private static final String GENERATED_FILE = "main.bal";

    public ReEvalExecutor() {
        super(TEMPLATE_FILE, new ReEvalState(), new ReEvalAsyncInvoker(GENERATED_FILE));
    }

    @Override
    public ReEvalContext currentContext(Snippet newSnippet) {
        List<String> imports = Context.snippetsToStrings(state.imports());
        List<String> moduleDeclarations = Context.snippetsToStrings(state.moduleDeclarations());
        List<String> variableDeclarations = Context.snippetsToStrings(state.variableDeclarations());
        List<ReEvalContext.StatementExpression> statementsAndExpressions = new ArrayList<>();
        ReEvalContext.StatementExpression newStatementOrExpression = new ReEvalContext.StatementExpression(newSnippet);
        List<String> importPrefixes = new ArrayList<>();

        // Add import prefixes
        for (Snippet importSnippet : state.imports()) {
            assert importSnippet instanceof ImportSnippet;
            importPrefixes.add(((ImportSnippet) importSnippet).getImportName());
        }

        // Add to correct category
        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            assert newSnippet instanceof ImportSnippet;
            imports.add(newSnippet.toSourceCode());
            importPrefixes.add(((ImportSnippet) newSnippet).getImportName());
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DECLARATION_KIND) {
            variableDeclarations.add(newSnippet.toSourceCode());
        }

        // Reformat expressions
        for (Snippet snippet : state.statementsAndExpressions()) {
            if (snippet != newSnippet) {
                statementsAndExpressions.add(new ReEvalContext.StatementExpression(snippet));
            }
        }

        return new ReEvalContext(imports, importPrefixes, moduleDeclarations,
                variableDeclarations, statementsAndExpressions, newStatementOrExpression);
    }

    @Override
    public boolean executeInvoker(Postprocessor postprocessor) throws IOException, InterruptedException {
        return invoker.execute(postprocessor);
    }
}
