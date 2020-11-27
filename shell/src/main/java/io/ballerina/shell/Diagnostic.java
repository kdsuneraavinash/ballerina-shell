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

import io.ballerina.tools.text.TextDocument;

/**
 * Diagnostic message which denotes error, debug messages.
 * These are collected and returned to the callee.
 * Can be used to determine a cause of an error.
 */
public class Diagnostic {
    private final String message;
    private final DiagnosticKind kind;

    private Diagnostic(String message, DiagnosticKind kind) {
        this.message = message;
        this.kind = kind;
    }

    /**
     * Creates an error diagnostic.
     *
     * @param message Reason for the error.
     * @return Error diagnostic message.
     */
    public static Diagnostic error(String message) {
        return new Diagnostic(message, DiagnosticKind.ERROR);
    }

    /**
     * Creates an warning diagnostic.
     *
     * @param message Reason for the warning.
     * @return Warning diagnostic message.
     */
    public static Diagnostic warn(String message) {
        return new Diagnostic(message, DiagnosticKind.WARN);
    }

    /**
     * Creates a debug diagnostic.
     *
     * @param message Debug message.
     * @return Debug diagnostic message.
     */
    public static Diagnostic debug(String message) {
        return new Diagnostic(message, DiagnosticKind.DEBUG);
    }

    /**
     * Highlight and show the error position.
     *
     * @param textDocument Text document to extract source code.
     * @param diagnostic   Diagnostic to show.
     * @return The string with position highlighted.
     */
    public static String highlightDiagnostic(TextDocument textDocument,
                                             io.ballerina.tools.diagnostics.Diagnostic diagnostic) {
        // Get the source code
        String space = " ";
        String sourceLine = textDocument.line(diagnostic.location().lineRange().startLine().line()).text();
        int position = diagnostic.location().lineRange().startLine().offset();
        return String.format("%s%n%s%n%s^", diagnostic.message(), sourceLine, space.repeat(position));
    }

    /**
     * The kind signifies the diagnostic type: error, warning, etc...
     *
     * @return Type of the diagnostic.
     */
    public DiagnosticKind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return message;
    }
}
