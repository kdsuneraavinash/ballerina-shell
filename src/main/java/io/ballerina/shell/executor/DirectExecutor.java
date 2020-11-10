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
package io.ballerina.shell.executor;

import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.shell.postprocessor.PostProcessor;
import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.wrapper.Wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * An executor that will delegate the process to the external ballerina executable command.
 * Ballerina must be installed and on path to use this executor.
 * This would directly call the ballerina command.
 */
public class DirectExecutor implements Executor {
    public static final String TEMP_FILE = "._main_exec.bal";
    private final PostProcessor postProcessor;
    private final Wrapper wrapper;
    private final Stack<Snippet<?>> snippets;

    public DirectExecutor(Wrapper wrapper, PostProcessor postProcessor) {
        this.wrapper = wrapper;
        this.postProcessor = postProcessor;
        snippets = new Stack<>();
    }

    @Override
    public String execute(Snippet<?> newSnippet) {
        // Default values for all snippets
        List<Snippet<?>> importSnippets = new ArrayList<>();
        List<Snippet<?>> moduleDeclarationSnippets = new ArrayList<>();
        List<Snippet<?>> statementSnippets = new ArrayList<>();
        Snippet<?> expressionSnippet = newSnippet;

        // Add snippets to the relevant category.
        snippets.push(newSnippet);
        for (Snippet<?> snippet : snippets) {
            if (snippet.isPersistent()) {
                if (snippet.getKind() == Snippet.SnippetKind.IMPORT_KIND) {
                    importSnippets.add(snippet);
                } else if (snippet.getKind() == Snippet.SnippetKind.MODULE_MEMBER_DECLARATION_KIND
                        || snippet.getKind() == Snippet.SnippetKind.VARIABLE_DEFINITION_KIND) {
                    moduleDeclarationSnippets.add(snippet);
                } else if (snippet.getKind() == Snippet.SnippetKind.STATEMENT_KIND) {
                    statementSnippets.add(snippet);
                }
            }
        }
        snippets.pop();

        // If statement is not a expression, set the default printing value to null
        if (newSnippet.getKind() != Snippet.SnippetKind.EXPRESSION_KIND) {
            expressionSnippet = new ExpressionSnippet(
                    NodeFactory.createNilLiteralNode(
                            NodeFactory.createToken(SyntaxKind.OPEN_PAREN_TOKEN),
                            NodeFactory.createToken(SyntaxKind.CLOSE_PAREN_TOKEN)));
        }

        try {
            // Temporarily remove current snippet
            // We re-add after we confirm that it is valid
            createSourceCodeFile(importSnippets, moduleDeclarationSnippets,
                    statementSnippets, expressionSnippet);

            // Execute and return correct output.
            String command = String.format("ballerina run %s", TEMP_FILE);
            ProcessInvoker processInvoker = new ShellCommandProcessInvoker(command);
            processInvoker.execute();

            String output = String.join("\n", processInvoker.getStandardError());
            if (!processInvoker.isErrorExit()) {
                snippets.add(newSnippet);
                output = String.join("\n", processInvoker.getStandardOutput());
            }
            return postProcessor.process(processInvoker.isErrorExit(), output);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Generate and dump the source file in the TEMP_FILE location.
     *
     * @param importSnippets            Import statements.
     * @param moduleDeclarationSnippets Module declarations.
     * @param statementSnippets         Statements.
     * @param expressionSnippet         Expression  to output.
     * @throws IOException If source file creation failed.
     */
    private void createSourceCodeFile(Collection<Snippet<?>> importSnippets,
                                      Collection<Snippet<?>> moduleDeclarationSnippets,
                                      Collection<Snippet<?>> statementSnippets,
                                      Snippet<?> expressionSnippet) throws IOException {
        SyntaxTree syntaxTree = wrapper.wrap(
                importSnippets, moduleDeclarationSnippets,
                statementSnippets, expressionSnippet
        );
        File file = new File(TEMP_FILE);
        try (FileWriter fileWriter = new FileWriter(file, Charset.defaultCharset())) {
            fileWriter.write(syntaxTree.toSourceCode());
        }
    }
}
