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
import io.ballerina.shell.executor.ExecutorResult;

import java.util.Scanner;

/**
 * Will output STDERR and STDOUT to the controller.
 * Will use error and warning prefixes to categorize STDERR.
 */
public class BasicPostprocessor implements Postprocessor {
    private static final String ERROR_PREFIX = "error:";
    private static final String WARNING_PREFIX = "warning:";

    @Override
    public boolean process(ExecutorResult executorResult, ShellController controller) {
        Scanner executorOut = new Scanner(executorResult.getStdErrLogs());

        // Process all output lines from the executor.
        boolean foundErrorsOrWarnings = false;
        while (executorOut.hasNextLine()) {
            String line = executorOut.nextLine();

            if (line.startsWith(ERROR_PREFIX)) {
                foundErrorsOrWarnings = true;
                controller.emitResult(line, LogStatus.ERROR);
            } else if (line.startsWith(WARNING_PREFIX)) {
                foundErrorsOrWarnings = true;
                controller.emitResult(line, LogStatus.WARNING);
            }
        }

        // Oh no; no error logs but failed.
        // Restore original logs, dont care if there are compiler/run messages.
        if (executorResult.isError() && !foundErrorsOrWarnings) {
            controller.emitResult(executorResult.getStdErrLogs(), LogStatus.FATAL_ERROR);
        }

        // Output from the STDOUT.
        controller.emitResult(executorResult.getStdOutLogs(), LogStatus.SUCCESS);
        return true;
    }
}
