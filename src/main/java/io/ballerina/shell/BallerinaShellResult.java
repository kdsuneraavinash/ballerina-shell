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
package io.ballerina.shell;

/**
 * Class for the shell results of the Ballerina Shell.
 * Contains each segment of a shell output.
 * If the shell input contained n statements,
 * there would be n shell results.
 */
public class BallerinaShellResult {
    private final String output;
    private final boolean isError;

    public BallerinaShellResult(String output, boolean isError) {
        this.output = output;
        this.isError = isError;
    }

    /**
     * Output of ballerina evaluator is either its error messages
     * or output. {@code isError} defines what it is.
     *
     * @return Output from the shell.
     */
    public String getOutput() {
        return output;
    }

    /**
     * This is true if the evaluation failed for some reason.
     *
     * @return Whether the ballerina evaluator encountered an error.
     */
    public boolean isError() {
        return isError;
    }
}
