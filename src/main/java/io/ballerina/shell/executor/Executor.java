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

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Executor that executes a list of snippets.
 * <p>
 * State of an executor persists all the information required.
 * It is preferred that no other state variable exists in the executor.
 * If there are, override the {@code reset} function.
 * <p>
 * Context of an executor is the context that will be used to
 * fill the mustache template. This should be a logic-less class.
 * Logic should be added in {@code currentContext} getter.
 * <p>
 * Invoker is the object that will launch the application.
 * That logic as well as logic to update state may be moved to the invoker.
 *
 * @param <P> State that the executor uses.
 * @param <Q> Context that the executor uses.
 * @param <R> Invoker that the executor uses.
 */
public abstract class Executor<P extends State, Q extends Context, R extends Invoker> {
    private static final String GENERATED_FILE = "main.bal";

    private final Template template;
    protected final P state;
    protected final R invoker;

    protected Executor(String templateName, P state, R invoker) {
        this.state = state;
        this.invoker = invoker;

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setClassForTemplateLoading(getClass(), "/");
        cfg.setDefaultEncoding("UTF-8");
        try {
            this.template = cfg.getTemplate(templateName);
        } catch (IOException e) {
            throw new ExecutorException(e);
        }

        String message = String.format("Using %s with %s invoker on %s file.",
                getClass().getSimpleName(), invoker.getClass().getSimpleName(), templateName);
        PrinterProvider.debug(message);
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
                template.process(context, fileWriter);
            }
            boolean isSuccess = executeInvoker(postprocessor);
            if (isSuccess) {
                onSuccess(newSnippet);
            }
            return isSuccess;
        } catch (Exception e) {
            PrinterProvider.debug("Process invoker/File generator failed!");
            throw new ExecutorException(e);
        }
    }

    /**
     * Creates a context that contain the current state and the new snippet.
     *
     * @param newSnippet New snippet to include.
     * @return Created context.
     */
    public abstract Q currentContext(Snippet newSnippet);

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

    public abstract void onSuccess(Snippet newSnippet);
}
