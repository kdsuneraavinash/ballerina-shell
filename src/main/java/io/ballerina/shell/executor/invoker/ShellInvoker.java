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

package io.ballerina.shell.executor.invoker;

import io.ballerina.shell.exceptions.ExecutorException;
import io.ballerina.shell.executor.Invoker;
import io.ballerina.shell.postprocessor.Postprocessor;

import java.io.IOException;

/**
 * Interface for calling external processes for {@link io.ballerina.shell.executor.reeval.ReEvalExecutor}.
 */
public abstract class ShellInvoker implements Invoker {
    /**
     * Executes the command line shell command/external process.
     * If something failed, the return would be {@code false}. (eg: compilation failed)
     *
     * @param postprocessor Postprocessor to use to output data.
     * @return Whether the operation was successful.
     * @throws IOException          When input read fails.
     * @throws InterruptedException WHen process executor/failed.
     */
    public abstract boolean execute(Postprocessor postprocessor) throws IOException, InterruptedException, ExecutorException;
}
