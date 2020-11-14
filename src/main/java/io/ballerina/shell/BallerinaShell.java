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

package io.ballerina.shell;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.ExecutorResult;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.preprocessor.Preprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetFactory;
import io.ballerina.shell.transformer.Transformer;
import io.ballerina.shell.treeparser.TreeParser;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.utils.timeit.TimeIt;

import java.util.List;

/**
 * Main shell entry point.
 * Creates an virtual shell which will accept input from
 * a terminal and evaluate each expression.
 */
public class BallerinaShell {
    private final Preprocessor preprocessor;
    private final TreeParser parser;
    private final Transformer transformer;
    private final Executor executor;
    private final Postprocessor postprocessor;

    /**
     * Creates a {@code BallerinaShell} evaluator.
     *
     * @param preprocessor  Preprocessor to use.
     * @param parser        Parser to use.
     * @param transformer   Node to snippet transformer to use.
     * @param executor      Executor to use.
     * @param postprocessor Post processor to use.
     */
    public BallerinaShell(Preprocessor preprocessor, TreeParser parser,
                          Transformer transformer, Executor executor,
                          Postprocessor postprocessor) {
        this.preprocessor = preprocessor;
        this.parser = parser;
        this.transformer = transformer;
        this.executor = executor;
        this.postprocessor = postprocessor;
    }

    /**
     * Base evaluation function which evaluates an input line.
     * <p>
     * An input line may contain one or more statements separated by semicolons.
     * The result will be written via the {@code ShellResultController}.
     * Each stage of the evaluator will be notified wia the controller.
     * <p>
     * If the execution failed, an error will be thrown instead.
     * If there was an error in one of the statements in the line,
     * then this will stop execution without evaluating later lines.
     *
     * @param source     Input line from user.
     * @param controller Shell result object which contain
     *                   the results of the shell after the execution is done.
     */
    public void evaluate(String source, ShellController controller) {
        try {
            controller.startSession();

            // 1. Preprocessor  - split the line into one or more statements.
            // 2. Parser        - Identify the nodes correctly.
            // 3. Convert       - Convert the node into a snippet.
            // 4. Transform     - Transform the snippet if needed.
            // 5. Executor      - Run the snippet.
            // 6. Postprocess   - Reformat the output from the executor.
            List<String> statements = TimeIt.timeIt("Preprocessor", () -> preprocessor.preprocess(source));
            for (String statement : statements) {
                Node rootNode = TimeIt.timeIt("Parser", () -> parser.parse(statement));
                Snippet snippet = TimeIt.timeIt("Snippet Factory", () -> SnippetFactory.fromNode(rootNode));
                ShellDiagnosticProvider.sendMessage("Identified as %s.", snippet.toString());
                Snippet transformedSnippet = TimeIt.timeIt("Transformer", () -> transformer.transform(snippet));
                ExecutorResult executorResult = TimeIt.timeIt("Executor", () -> executor.execute(transformedSnippet));
                TimeIt.timeIt("Postprocessor", () -> postprocessor.process(executorResult, controller));

                if (executorResult.isError()) {
                    throw new ExecutorException();
                }
            }

            controller.finishSession();
        } catch (ExecutorException e) {
            // Statement in the multiple statement input failed.
            controller.failSession();
        } catch (Exception e) {
            // Unexpected error.
            controller.failSession();
            throw e;
        }
    }
}
