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

package io.ballerina.shell.invoker;

import io.ballerina.shell.DiagnosticReporter;
import io.ballerina.shell.exceptions.InvokerException;
import io.ballerina.shell.snippet.Snippet;

/**
 * Invoker that invokes a command to evaluate a list of snippets.
 * <p>
 * State of an invoker persists all the information required.
 * {@code reset} function will clear the invoker state.
 * <p>
 * Context of an executor is the context that will be used to
 * fill the template. This should be a logic-less as much as possible.
 * Invoker and its context may be tightly coupled.
 */
public abstract class Invoker extends DiagnosticReporter {
    /**
     * Initializes the invoker. This can be used to load required files
     * and create caches. Calling this is not a requirement.
     *
     * @throws InvokerException If initialization failed.
     */
    public abstract void initialize() throws InvokerException;

    /**
     * Reset executor state so that the execution can be start over.
     */
    public abstract void reset();

    /**
     * Executes a snippet and returns the output lines.
     * Snippets parameter should only include newly added snippets.
     * Old snippets should be managed as necessary by the implementation.
     *
     * @param newSnippet New snippet to execute.
     * @return Execution output lines.
     */
    public abstract boolean execute(Snippet newSnippet) throws InvokerException;
}
