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

import java.io.IOException;
import java.util.List;

/**
 * Interface for calling external processes.
 */
public interface ProcessInvoker {
    /**
     * Executes the command line binary.
     *
     * @throws IOException          When input read fails.
     * @throws InterruptedException WHen process executor/failed.
     */
    void execute() throws IOException, InterruptedException;

    /**
     * Whether the process exited in an error.
     *
     * @return Is process final state an error.
     */
    boolean isErrorExit();

    List<String> getStandardError();

    List<String> getStandardOutput();
}
