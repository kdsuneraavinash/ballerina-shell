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

import java.util.Stack;

/**
 * Executes a list of snippets.
 * The stateless nature of the executor comes because the
 * compiler itself wont preserve state.
 * The syntax tree would be re-evaluated at each statement
 * resulting in major performance losses as well as bugs when
 * randoms are involved.
 */
public abstract class StatelessExecutor implements Executor {
    protected final Stack<Snippet> snippets;
    protected final Wrapper wrapper;

    protected StatelessExecutor(Wrapper wrapper) {
        this.wrapper = wrapper;
        this.snippets = new Stack<>();
    }

    @Override
    public ExecutorResult execute(Snippet newSnippet) {
        // Add snippet to process
        ExpressionSnippet expressionSnippet = null;
        snippets.push(newSnippet);
        if (newSnippet instanceof ExpressionSnippet) {
            expressionSnippet = (ExpressionSnippet) newSnippet;
        }

        boolean isExecutionError = false;
        try {
            // Evaluate the wrapped source code
            String sourceCode = wrapper.wrap(snippets, expressionSnippet);
            ExecutorResult executorResult = evaluateSourceCode(sourceCode);
            isExecutionError = executorResult.isError();
            return executorResult;
        } catch (Exception e) {
            isExecutionError = true;
            throw new RuntimeException("Code evaluation failed!", e);
        } finally {
            // Remove snippet from the stack if error
            if (isExecutionError) {
                snippets.pop();
            }
        }
    }

    /**
     * Method to evaluate a whole string of source code.
     *
     * @param sourceCode Source code to evaluate.
     * @return Evaluation result.
     */
    protected abstract ExecutorResult evaluateSourceCode(String sourceCode);
}
