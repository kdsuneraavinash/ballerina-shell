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

package io.ballerina.shell.diagnostics;

import java.util.Objects;

/**
 * Debug information collection class.
 * This is a singleton. Call {@code addDebugMessage} to add a message.
 * Messages can also be cleared.
 */
public final class ShellDiagnosticProvider {
    private static ShellDiagnosticProvider instance;
    private DiagnosticWriter diagnosticWriter;

    private ShellDiagnosticProvider() {
        setWriter(null);
    }

    /**
     * Singleton instance reference get method.
     *
     * @return Instance reference.
     */
    public static synchronized ShellDiagnosticProvider getInstance() {
        if (instance == null) {
            instance = new ShellDiagnosticProvider();
            instance.setWriter(null);
        }
        return instance;
    }

    /**
     * Add a new debug message with a default null provider.
     *
     * @param messageTemplate Debug message content template.
     * @param arguments       Values to populate the template.
     */
    public static void sendMessage(String messageTemplate, String... arguments) {
        getInstance().sendMessage(null, messageTemplate, arguments);
    }

    /**
     * Add a new debug message.
     *
     * @param provider        Class that provides the message.
     * @param messageTemplate Debug message content template.
     * @param arguments       Values to populate the template.
     */
    public void sendMessage(Class<?> provider, String messageTemplate, String... arguments) {
        ShellDiagnosticMessage diagnosticMessage = new ShellDiagnosticMessage(provider,
                String.format(messageTemplate, (Object[]) arguments));
        diagnosticWriter.write(diagnosticMessage.toString());
    }

    /**
     * Set the writer. Disables writing if null.
     */
    public void setWriter(DiagnosticWriter diagnosticWriter) {
        this.diagnosticWriter = Objects.requireNonNullElse(diagnosticWriter, new DefaultDiagnosticWriter());
    }
}
