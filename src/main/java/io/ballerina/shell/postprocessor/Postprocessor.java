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

package io.ballerina.shell.postprocessor;

import io.ballerina.shell.ShellController;
import io.ballerina.shell.executor.ExecutorResult;

/**
 * Processes a string output to a processed string.
 * May perform some filtering, mapping on the output string.
 */
public interface Postprocessor {
    /**
     * Processes the output depending on the exit type.
     * Every output from postprocessor is sent via controller.
     *
     * @param executorResult Result of the executor
     * @param controller     Shell controller to output the output.
     * @return Whether the preprocessing encountered errors.
     */
    boolean process(ExecutorResult executorResult, ShellController controller);
}
