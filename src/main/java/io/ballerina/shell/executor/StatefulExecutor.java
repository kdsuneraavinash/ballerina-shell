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

import io.ballerina.shell.executor.wrapper.Wrapper;
import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.Snippet;

import java.util.List;

/**
 * Executes a list of snippets.
 * The Stateful nature of the executor comes because the
 * compiler itself will preserve state.
 * The syntax tree may or may not be be re-evaluated.
 * However, this would provide only the most recent snippet,
 */
public abstract class StatefulExecutor implements Executor {
    protected final Wrapper wrapper;

    protected StatefulExecutor(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public ExecutorResult execute(Snippet newSnippet) {
        try {
            // Add snippet to process
            ExpressionSnippet expressionSnippet = null;
            if (newSnippet instanceof ExpressionSnippet) {
                expressionSnippet = (ExpressionSnippet) newSnippet;
            }

            // Evaluate the wrapped source code
            List<Snippet> snippets = getSnippetsForExecution(newSnippet);
            String sourceCode = wrapper.wrap(snippets, expressionSnippet);
            ExecutorResult result = evaluateSourceCode(sourceCode);
            if (result.isError()) {
                executionFailed(newSnippet);
                return result;
            }
            executionSuccessful(newSnippet);
            return result;
        } catch (Exception e) {
            executionFailed(newSnippet);
            throw new RuntimeException("Code evaluation failed!", e);
        }
    }

    /**
     * Method to evaluate a whole string of source code.
     * Here the implementation may need to preserve after state.
     *
     * @param sourceCode Source code to evaluate.
     * @return Evaluation result.
     */
    protected abstract ExecutorResult evaluateSourceCode(String sourceCode);

    /**
     * Signals the start of execution.
     * Generates the snippets that would restore the state to the current state.
     * Only the new snippet is provided. Implementation should take care to keep track of the state.
     *
     * @param newSnippet Newly added snippet.
     * @return List of snippets to evaluate.
     */
    protected abstract List<Snippet> getSnippetsForExecution(Snippet newSnippet);

    /**
     * Will be called if execution was successful.
     *
     * @param newSnippet New that was executed.
     */
    protected abstract void executionSuccessful(Snippet newSnippet);

    /**
     * Will be called if execution errored.
     *
     * @param newSnippet New that was executed.
     */
    protected abstract void executionFailed(Snippet newSnippet);
}
