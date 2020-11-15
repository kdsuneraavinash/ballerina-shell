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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.io.FileWriter;
import java.nio.charset.Charset;

/**
 * Executor that executes a list of snippets.
 *
 * @param <P> State that the executor uses.
 * @param <Q> Invoker that the executor uses.
 */
public abstract class Executor<P extends State, Q extends Invoker> {
    private static final String GENERATED_FILE = "main.bal";

    private final Mustache mustache;
    protected final P state;
    protected final Q invoker;

    protected Executor(String templateName, P state, Q invoker) {
        this.state = state;
        this.invoker = invoker;
        MustacheFactory mf = new DefaultMustacheFactory();
        mustache = mf.compile(templateName);

        String message = String.format("Using %s with %s invoker on %s file.",
                getClass().getSimpleName(), invoker.getClass().getSimpleName(), templateName);
        DebugProvider.sendMessage(message);
    }

    /**
     * Executes a snippet and returns the output lines.
     * Snippets parameter should only include newly added snippets.
     * Old snippets should be managed as necessary by the implementation.
     *
     * @param newSnippet New snippet to execute.
     * @return Execution output lines.
     */
    public boolean execute(Snippet newSnippet, Postprocessor postprocessor) {
        // 1. Populate context
        // 2. Generate file using context
        // 3. Execute the invoker
        // 4. If success, permanently ass snippet to state
        try {
            Context context = currentContext(newSnippet);
            try (FileWriter fileWriter = new FileWriter(GENERATED_FILE, Charset.defaultCharset())) {
                mustache.execute(fileWriter, context).flush();
            }
            boolean isSuccess = executeInvoker(postprocessor);
            if (isSuccess) {
                state.addSnippet(newSnippet);
            }
            return isSuccess;
        } catch (Exception e) {
            DebugProvider.sendMessage("Process invoker/File generator failed!");
            throw new ExecutorException(e);
        }
    }

    /**
     * Creates a context that contain the current state and the new snippet.
     *
     * @param newSnippet New snippet to include.
     * @return Created context.
     */
    public abstract Context currentContext(Snippet newSnippet);

    /**
     * Executes the invoker and returns whether it was successful.
     *
     * @param postprocessor Postprocessor for the invoker.
     * @return Whether execution was successful.
     */
    public abstract boolean executeInvoker(Postprocessor postprocessor) throws Exception;

    /**
     * Reset executor state so that the execution can be start over.
     */
    public void reset() {
        state.reset();
    }
}
