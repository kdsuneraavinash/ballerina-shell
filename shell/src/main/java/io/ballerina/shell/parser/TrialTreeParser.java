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

import java.util.List;
import java.util.Objects;

/**
 * Parses the source code line using a trial based method.
 * The source code is placed in several places and is attempted to parse.
 * This continues until the correct type can be determined.
 */
public class TrialTreeParser extends TreeParser {
    private static final long DEFAULT_TIMEOUT_MS = 100;
    private final List<TreeParserTrial> nodeParserTrials;
    private final long timeOutDurationMs;

    public TrialTreeParser(long timeOutDurationMs) {
        this.timeOutDurationMs = timeOutDurationMs;
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

    public TrialTreeParser() {
        this(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public Node parse(String source) throws TreeParserException {
        String errorMessage = "";
        for (TreeParserTrial trial : nodeParserTrials) {
            try {
                return Objects.requireNonNull(trial.parse(source), "trial returned no nodes");
            } catch (ParserTrialFailedException e) {
                errorMessage = e.getMessage();
                addDiagnostic(Diagnostic.debug(String.format("Failed %s because %s",
                        trial.getClass().getSimpleName(), errorMessage)));
            } catch (ParserRejectedException e) {
                errorMessage = "Invalid statement: " + e.getMessage();
                break;
            } catch (Exception e) {
                errorMessage = "Invalid statement. Could not parse the expression: " + e.getMessage();
                addDiagnostic(Diagnostic.debug(String.format("Unexpected Fail %s because %s",
                        trial.getClass().getSimpleName(), errorMessage)));
            } catch (Error e) {
                errorMessage = "Something severely went wrong: " + e.toString();
            }
        }
        addDiagnostic(Diagnostic.error(errorMessage));
        addDiagnostic(Diagnostic.error("Parsing aborted because of errors."));
        throw new TreeParserException();
    }

    public long getTimeOutDurationMs() {
        return timeOutDurationMs;
    }
}
