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
 * Public interface for the shell result of the Ballerina Shell.
 */
public interface ShellResultController {
    /**
     * Adds a shell results to the output.
     * Child classes may override this to fetch the shell results.
     *
     * @param ballerinaShellResultPart current shell result.
     */
    void addBallerinaShellResult(BallerinaShellResult ballerinaShellResultPart);

    /**
     * Ends a execution session.
     * A session contains one user line and one or more statements.
     * This will be not called if there was an error in execution.
     */
    void completeExecutionSession();
}
