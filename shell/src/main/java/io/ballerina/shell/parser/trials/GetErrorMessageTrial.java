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

package io.ballerina.shell.parser.trials;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.Set;

/**
 * This will process without timing out so we can get a
 * better error message.
 * Otherwise error will be just 'timed out'
 */
public class GetErrorMessageTrial extends TreeParserTrial {
    private static final long LONG_TIME_OUT_DURATION_MS = 1000;
    private static final Set<String> MODULE_LEVEL_ERROR_CODES = Set.of("BCE0007", "BCE0646");

    @Override
    public Node parse(String source) throws ParserTrialFailedException {
        TextDocument document = TextDocuments.from(String.format("function main(){%n%s%n}", source));
        SyntaxTree tree = SyntaxTree.from(document);
        for (Diagnostic diagnostic : tree.diagnostics()) {
            DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
            if (diagnosticInfo.severity() == DiagnosticSeverity.ERROR) {
                if (MODULE_LEVEL_ERROR_CODES.contains(diagnosticInfo.code())) {
                    break;
                }
                throw new ParserTrialFailedException(tree.textDocument(), diagnostic);
            }
        }

        // We got to parse as a top level module dcln
        tree = SyntaxTree.from(TextDocuments.from(source));
        for (Diagnostic diagnostic : tree.diagnostics()) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                throw new ParserTrialFailedException(tree.textDocument(), diagnostic);
            }
        }

        // If no error, still throw
        throw new ParserTrialFailedException("Unknown statement");
    }

    @Override
    public long getTimeOutDurationMs() {
        return LONG_TIME_OUT_DURATION_MS;
    }
}
