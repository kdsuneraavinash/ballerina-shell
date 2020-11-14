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

package io.ballerina.shell.treeparser.trials;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.Objects;

/**
 * Attempts to parse source as a statement.
 * Puts in the main function statement level and checks for the the entry.
 */
public class ExpressionTrial implements TreeParserTrial {
    @Override
    public Node tryParse(String source) throws ParserTrialFailedException {
        try {
            String sourceCode = String.format("function main(){return %s}", source);

            TextDocument document = TextDocuments.from(sourceCode);
            SyntaxTree tree = SyntaxTree.from(document);

            for (Diagnostic diagnostic : tree.diagnostics()) {
                if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                    throw new Exception(diagnostic.message());
                }
            }

            ModulePartNode node = tree.rootNode();

            ModuleMemberDeclarationNode moduleDeclaration = node.members().get(0);
            Objects.requireNonNull(moduleDeclaration);
            FunctionDefinitionNode mainFunction = (FunctionDefinitionNode) moduleDeclaration;
            FunctionBodyBlockNode mainFunctionBody = (FunctionBodyBlockNode) mainFunction.functionBody();
            ReturnStatementNode returnStatement = (ReturnStatementNode) mainFunctionBody.statements().get(0);
            if (returnStatement.expression().isEmpty()) {
                throw new ParserTrialFailedException("Return statement not parsed");
            }
            return returnStatement.expression().get();
        } catch (Exception e) {
            throw new ParserTrialFailedException(e);
        }
    }
}
