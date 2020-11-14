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

import io.ballerina.shell.LogStatus;
import io.ballerina.shell.ShellController;

/**
 * Will output STDERR and STDOUT to the controller directly without processing.
 */
public class NonePostProcessor extends Postprocessor {
    public NonePostProcessor(ShellController controller) {
        super(controller);
    }

    @Override
    public void onProgramOutput(String line) {
        controller.emitResult(line, LogStatus.SUCCESS);
    }

    @Override
    public void onCompilerOutput(String line) {
        controller.emitResult(line, LogStatus.ERROR);
    }
}