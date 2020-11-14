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
    private final String stdErrLogs;
    private final String stdOutLogs;

    public ExecutorResult(boolean isError, String stdErrLogs, String stdOutLogs) {
        this.isError = isError;
        this.stdErrLogs = stdErrLogs;
        this.stdOutLogs = stdOutLogs;
    }

    /**
     * Output from the executor to the standard error.
     * However, having output in this doesn't mean that the
     * program execution failed. Execution status is given via {@code isError}.
     *
     * @return STDERR of the executor.
     */
    public String getStdErrLogs() {
        return stdErrLogs;
    }

    /**
     * Output from the executor to the standard output.
     *
     * @return STDOUT of the executor.
     */
    public String getStdOutLogs() {
        return stdOutLogs;
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
