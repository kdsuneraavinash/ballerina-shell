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

/**
 * Container for the executor output.
 * Contains the executor output strings and whether
 * the executor countered an error.
 */
public class ExecutorResult {
    private final boolean isError;
    private final String output;

    public ExecutorResult(boolean isError, String output) {
        this.isError = isError;
        this.output = output;
    }

    /**
     * Output of an executor is either its error messages
     * or output. {@code isError} defines what it is.
     *
     * @return Output of the executor.
     */
    public String getOutput() {
        return output;
    }

    /**
     * This is true if the execution failed for some reason.
     *
     * @return Whether the executor encountered an error.
     */
    public boolean isError() {
        return isError;
    }
}
