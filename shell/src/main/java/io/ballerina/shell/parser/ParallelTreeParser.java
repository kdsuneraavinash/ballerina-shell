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

package io.ballerina.shell.parser;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.exceptions.TreeParserException;
import io.ballerina.shell.parser.trials.EmptyExpressionTrial;
import io.ballerina.shell.parser.trials.ExpressionTrial;
import io.ballerina.shell.parser.trials.GetErrorMessageTrial;
import io.ballerina.shell.parser.trials.ImportDeclarationTrial;
import io.ballerina.shell.parser.trials.ModuleMemberTrial;
import io.ballerina.shell.parser.trials.ParserRejectedException;
import io.ballerina.shell.parser.trials.ParserTrialFailedException;
import io.ballerina.shell.parser.trials.RejectInvalidStmtTrial;
import io.ballerina.shell.parser.trials.StatementTrial;
import io.ballerina.shell.parser.trials.TreeParserTrial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Parses the source code line using a trial based method.
 * The source code is placed in several places and is attempted to parse.
 * This continues until the correct type can be determined.
 * <p>
 * Task is parallelized in this parser.
 * The first trial that passes is accepted.
 * However, due to high CPU insensitivity in parsing, the
 * performance is lower than that of {@link SerialTreeParser}.
 */
public class ParallelTreeParser extends TrialTreeParser {
    private final List<TreeParserTrial> nodeParserTrials;

    public ParallelTreeParser(long timeOutDurationMs) {
        super(timeOutDurationMs);
        this.nodeParserTrials = List.of(
                new ImportDeclarationTrial(this),
                new RejectInvalidStmtTrial(this),
                new ModuleMemberTrial(this),
                new ExpressionTrial(this),
                new StatementTrial(this),
                new EmptyExpressionTrial(this),
                new GetErrorMessageTrial(this)
        );
    }

    @Override
    public Node parse(String source) throws TreeParserException {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        List<Callable<Node>> tasks = new ArrayList<>();
        AtomicReference<String> errorMessage = new AtomicReference<>();

        for (TreeParserTrial trial : nodeParserTrials) {
            tasks.add(() -> parse(trial, source, errorMessage));
        }
        try {
            Node node = executorService.invokeAny(tasks, getTimeOutDurationMs(), TimeUnit.MILLISECONDS);
            Objects.requireNonNull(node, "Parser returned no nodes");
            return node;
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
        }
        addDiagnostic(Diagnostic.error(errorMessage.get()));
        addDiagnostic(Diagnostic.error("Parsing aborted because of errors."));
        throw new TreeParserException();
    }

    /**
     * Parses a string to a node using the given trial parser.
     *
     * @param trial        Trial to run.
     * @param source       Source to run trial against.
     * @param errorMessage Error message setter.
     * @return Parsed node if any.
     */
    private Node parse(TreeParserTrial trial, String source, AtomicReference<String> errorMessage) {
        try {
            Node parsedNode = trial.parse(source);
            Objects.requireNonNull(parsedNode, "Trial returned no nodes");
            return parsedNode;
        } catch (ParserTrialFailedException e) {
            errorMessage.set(e.getMessage());
        } catch (ParserRejectedException e) {
            errorMessage.set("Invalid statement: " + e.getMessage());
        } catch (Exception e) {
            errorMessage.set("Invalid statement. Could not parse the expression: " + e.getMessage());
        } catch (Error e) {
            errorMessage.set("Something severely went wrong in parsing: " + e.toString());
        }
        throw new RejectedExecutionException();
    }
}
