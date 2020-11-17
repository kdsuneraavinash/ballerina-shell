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
import io.ballerina.shell.PrinterProvider;

/**
 * Will output STDERR and STDOUT to the controller.
 * Will use error and warning prefixes to categorize STDERR.
 * TODO: Use Project API diagnostics instead.
 */
public class BasicPostProcessor implements Postprocessor {
    private static final String ERROR_PREFIX = "error:";
    private static final String WARNING_PREFIX = "warning:";
    private static final String EXPECTED_COMPILING_MSG = "Compiling source";
    private static final String EXPECTED_PROGRAM_NAME = "main.bal";
    private static final String EXPECTED_RUNNING_MSG = "Running executables";

    private boolean isActivated;

    @Override
    public void onProgramOutput(String line) {
        if (line.trim().equals(ACTIVATION_START)) {
            isActivated = true;
        } else if (line.trim().equals(ACTIVATION_END)) {
            isActivated = false;
        } else if (isActivated) {
            PrinterProvider.emit(line, LogStatus.SUCCESS);
        }
    }

    @Override
    public void onCompilerOutput(String line) {
        if (line.startsWith(ERROR_PREFIX)) {
            PrinterProvider.emit(line, LogStatus.ERROR);
        } else if (line.startsWith(WARNING_PREFIX)) {
            PrinterProvider.emit(line, LogStatus.WARNING);
        } else {
            String stripped = line.trim();
            if (stripped.isBlank()
                    || stripped.startsWith(EXPECTED_COMPILING_MSG)
                    || stripped.startsWith(EXPECTED_PROGRAM_NAME)
                    || stripped.startsWith(EXPECTED_RUNNING_MSG)) {
                return;
            }
            // Unexpected line
            PrinterProvider.emit(line, LogStatus.FATAL_ERROR);
        }
    }
}
