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
 * Debug information collection class.
 * Call {@code addDebugMessage} to add a message.
 * Messages can also be cleared.
 */
public class PrinterProvider {
    private static PrinterService printerService;

    /**
     * Add a new debug message.
     *
     * @param message Debug message content template.
     */
    public static void debug(String message) {
        emit(message, LogStatus.DEBUG);
    }

    /**
     * Add a new message.
     *
     * @param message Message to output.
     * @param status  Status of the log. (error/debug/warning/...)
     */
    public static void emit(String message, LogStatus status) {
        if (printerService != null) {
            printerService.write(message, status);
        }
    }

    /**
     * Set the writer. Disables writing if null.
     */
    public static void setWriter(PrinterService printerService) {
        PrinterProvider.printerService = printerService;
    }
}
