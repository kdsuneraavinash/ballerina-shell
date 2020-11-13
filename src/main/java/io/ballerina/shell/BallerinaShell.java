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
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.ExecutorResult;
import io.ballerina.shell.postprocessor.BasicPostprocessor;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.preprocessor.CombinedPreprocessor;
import io.ballerina.shell.preprocessor.Preprocessor;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.transformer.MasterTransformer;
import io.ballerina.shell.treeparser.TreeParser;
import io.ballerina.shell.treeparser.TrialTreeParser;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.utils.timeit.TimeIt;

import java.util.List;

/**
 * Main shell entry point.
 */
public class BallerinaShell {
    private final Preprocessor preprocessor;
    private final TreeParser parser;
    private final MasterTransformer transformer;
    private final Executor executor;
    private final Postprocessor postprocessor;

    public BallerinaShell(Executor executor) {
        this.preprocessor = new CombinedPreprocessor(new SeparatorPreprocessor());
        this.parser = new TrialTreeParser();
        this.transformer = new MasterTransformer();
        this.executor = executor;
        this.postprocessor = new BasicPostprocessor();
    }

    /**
     * Base evaluation function. Evaluates a input line.
     * If the evaluation fails for one statement, the this will stop execution.
     *
     * @param input                 Input line from user.
     * @param shellResultController Shell result object which contain
     *                              the results of the shell after the execution is done.
     */
    public void evaluate(String input, ShellResultController shellResultController) throws Exception {
        List<String> source = preprocessor.preprocess(input);
        for (String sourceLine : source) {
            Node rootNode = TimeIt.timeIt("Parser", () -> parser.parse(sourceLine));
            Snippet<?> snippet = TimeIt.timeIt("Transformer", () -> transformer.transform(rootNode));

            ShellDiagnosticProvider.sendMessage(
                    "Identified code as a %s node and a %s snippet.",
                    rootNode.getClass().getSimpleName(), snippet.getKind().toString());

            ExecutorResult executorResult = TimeIt.timeIt("Executor", () -> executor.execute(snippet));
            String output = TimeIt.timeIt("Postprocessor", () -> postprocessor.process(executorResult));

            BallerinaShellResult ballerinaShellResultPart = new BallerinaShellResult(output, executorResult.isError());
            shellResultController.addBallerinaShellResult(ballerinaShellResultPart);
            if (executorResult.isError()) {
                throw new ExecutorFailedException();
            }
        }
        shellResultController.completeExecutionSession();
    }
}
