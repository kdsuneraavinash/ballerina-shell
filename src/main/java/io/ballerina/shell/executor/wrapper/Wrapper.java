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
package io.ballerina.shell.executor.wrapper;

import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.ImportSnippet;
import io.ballerina.shell.snippet.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.StatementSnippet;
import io.ballerina.shell.snippet.VariableDefinitionSnippet;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Wraps the snippets in a code segment and
 * generate the corresponding syntax tree.
 */
public abstract class Wrapper {
    private static final String SPECIAL_DELIMITER = "\\A";

    private final String templateFile;

    protected Wrapper(String templateFile) {
        ShellDiagnosticProvider.sendMessage("Wrapper used with %s template file and %s wrapper.",
                templateFile, this.getClass().getSimpleName());
        this.templateFile = templateFile;
    }

    /**
     * Wraps the snippets.
     *
     * @param imports                     Import statement snippets.
     * @param moduleDeclarations          Module level declaration snippets.
     * @param variableDeclarationSnippets Variable declaration snippets.
     * @param statements                  Statement snippets to wrapped in a function body.
     * @param expression                  Expression statement that is evaluated.
     * @return Source code corresponding to the wrapped snippets.
     */
    public abstract String wrap(Collection<ImportSnippet> imports,
                                Collection<ModuleMemberDeclarationSnippet> moduleDeclarations,
                                Collection<VariableDefinitionSnippet> variableDeclarationSnippets,
                                Collection<StatementSnippet> statements, ExpressionSnippet expression);

    /**
     * Wraps the given snippets.
     * Here the expression snippets should also be in the snippets collection.
     * Expression snippet should be the most recent expression snippet.
     * (The snippet that was immediately given)
     * If there are no snippets or the most recent snippet is of other type pass null.
     *
     * @param snippets          All the snippets in code.
     * @param expressionSnippet Most recent expression snippet. (May be null)
     * @return Source code corresponding to the wrapped snippets.
     */
    public String wrap(Collection<Snippet<?>> snippets, ExpressionSnippet expressionSnippet) {
        // Default values for all snippets
        List<ImportSnippet> importSnippets = new ArrayList<>();
        List<ModuleMemberDeclarationSnippet> moduleDeclarationSnippets = new ArrayList<>();
        List<VariableDefinitionSnippet> variableDeclarationSnippets = new ArrayList<>();
        List<StatementSnippet> statementSnippets = new ArrayList<>();
        if (expressionSnippet == null) {
            // Default expression is a colored OK message
            expressionSnippet = ExpressionSnippet.fromNode(
                    NodeFactory.createBasicLiteralNode(
                            SyntaxKind.STRING_LITERAL,
                            NodeFactory.createLiteralValueToken(
                                    SyntaxKind.STRING_LITERAL_TOKEN,
                                    "\"\u001b[30;1mOK\u001b[0m\"",
                                    NodeFactory.createEmptyMinutiaeList(),
                                    NodeFactory.createEmptyMinutiaeList()
                            )
                    ));
        }

        // Add snippets to the relevant category.
        for (Snippet<?> snippet : snippets) {
            if (snippet instanceof ImportSnippet) {
                importSnippets.add((ImportSnippet) snippet);
            } else if (snippet instanceof ModuleMemberDeclarationSnippet) {
                moduleDeclarationSnippets.add((ModuleMemberDeclarationSnippet) snippet);
            } else if (snippet instanceof VariableDefinitionSnippet) {
                variableDeclarationSnippets.add((VariableDefinitionSnippet) snippet);
            } else if (snippet instanceof StatementSnippet) {
                statementSnippets.add((StatementSnippet) snippet);
            }
        }

        return wrap(importSnippets, moduleDeclarationSnippets, variableDeclarationSnippets,
                statementSnippets, expressionSnippet
        );
    }

    /**
     * Read the template for the code gen.
     *
     * @return Read the template from a resource file.
     */
    protected String readTemplate() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(templateFile);
        Objects.requireNonNull(inputStream, "File open failed");
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }
}
