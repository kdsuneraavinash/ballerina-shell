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

import io.ballerina.shell.executor.ExecutorResult;

import java.util.Scanner;

/**
 * Returns the output as is.
 */
public class BasicPostprocessor implements Postprocessor {
    private static final String[] WHITE_LISTED_STD_ERR_PREFIXES = {
            "error:", "warning:"
    };

    @Override
    public String process(ExecutorResult executorResult) {
        if (executorResult.isError()) {
            Scanner outputScanner = new Scanner(executorResult.getOutput());
            StringBuilder original = new StringBuilder();
            StringBuilder formatted = new StringBuilder();

            boolean foundAnyLogs = false;
            formatted.append("Compilation failed (Error Logs):\n");

            while (outputScanner.hasNextLine()) {
                String line = outputScanner.nextLine();
                original.append(line).append("\n");
                for (String prefix : WHITE_LISTED_STD_ERR_PREFIXES) {
                    if (line.startsWith(prefix)) {
                        formatted.append(line).append("\n");
                        foundAnyLogs = true;
                        break;
                    }
                }
            }
            if (!foundAnyLogs) {
                // Oh no; no error logs but failed
                // Restore original logs, dont care if there are compiler/run messages
                return original.toString();
            }

            return formatted.toString();
        } else {
            return executorResult.getOutput();
        }
    }
}
