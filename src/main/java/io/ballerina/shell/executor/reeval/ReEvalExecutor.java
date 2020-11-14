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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.invoker.AsyncProcessInvoker;
import io.ballerina.shell.executor.invoker.ProcessInvoker;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes the snippet given.
 * Re evaluates the snippet by generating a file containing all snippets
 * and executing it.
 */
public class ReEvalExecutor implements Executor {
    private static final String BALLERINA_COMMAND = "ballerina run %s";
    private static final String TEMPLATE_FILE = "reeval.bal.mustache";
    private static final String GENERATED_FILE = "main.bal";

    /**
     * S simple interface to define the type of operations for
     * {@code doOperationToRelevantList} method.
     *
     * @param <T> Type of the list.
     */
    private interface OperationToList<T> {
        void operate(List<T> list);
    }

    private final List<Snippet> imports;
    private final List<Snippet> moduleDeclarations;
    private final List<Snippet> variableDeclarations;
    private final List<Snippet> statementsAndExpressions;
    private final Mustache mustache;
    private final ProcessInvoker processInvoker;

    public ReEvalExecutor() {
        String command = String.format(BALLERINA_COMMAND, GENERATED_FILE);
        processInvoker = new AsyncProcessInvoker(command);
        DebugProvider.sendMessage("Using re-eval executor with shell process invoker.");
        DebugProvider.sendMessage("Shell command invocation used: " + command);

        imports = new ArrayList<>();
        moduleDeclarations = new ArrayList<>();
        variableDeclarations = new ArrayList<>();
        statementsAndExpressions = new ArrayList<>();
        MustacheFactory mf = new DefaultMustacheFactory();
        mustache = mf.compile(TEMPLATE_FILE);
    }

    @Override
    public boolean execute(Snippet newSnippet, Postprocessor postprocessor) {
        // Whether execution succeeded
        boolean isSuccess = false;

        // Add new snippet to relevant category
        doOperationToRelevantList(newSnippet, (list) -> list.add(newSnippet));

        try {
            // Get the new expression and populate context
            ReEvalContext context = ReEvalContext.create(imports, moduleDeclarations,
                    variableDeclarations, statementsAndExpressions, newSnippet);

            // Generate file
            try (FileWriter fileWriter = new FileWriter(GENERATED_FILE)) {
                mustache.execute(fileWriter, context).flush();
            }

            // Execute shell command
            isSuccess = processInvoker.execute(postprocessor);
            return isSuccess;

        } catch (InterruptedException | IOException e) {
            DebugProvider.sendMessage("Process invoker/File generator failed!");
            throw new ExecutorException(e);
        } finally {
            // Remove new snippet if new snippet was bad bad
            if (!isSuccess) {
                doOperationToRelevantList(newSnippet, (list) -> list.remove(newSnippet));
            }
        }
    }

    @Override
    public void reset() {
        DebugProvider.sendMessage("Resetting Executor.");
        imports.clear();
        moduleDeclarations.clear();
        variableDeclarations.clear();
        statementsAndExpressions.clear();
    }

    /**
     * Does a given operation on the correct array depending on the
     * type of new snippet. If the new snippet is a expression, that list would be operated.
     *
     * @param snippet   Snippet to check the type of.
     * @param operation Operation to do.
     */
    private void doOperationToRelevantList(Snippet snippet, OperationToList<Snippet> operation) {
        if (snippet.getKind() == SnippetKind.IMPORT_KIND) {
            operation.operate(imports);
        } else if (snippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            operation.operate(moduleDeclarations);
        } else if (snippet.getKind() == SnippetKind.VARIABLE_DEFINITION_KIND) {
            operation.operate(variableDeclarations);
        } else if (snippet.getKind() == SnippetKind.STATEMENT_KIND) {
            operation.operate(statementsAndExpressions);
        } else if (snippet.getKind() == SnippetKind.EXPRESSION_KIND) {
            operation.operate(statementsAndExpressions);
        } else {
            throw new RuntimeException("Unexpected operation type.");
        }
    }
}
