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

import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.ImportSnippet;
import io.ballerina.shell.snippet.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.StatementSnippet;
import io.ballerina.shell.snippet.VariableDefinitionSnippet;

import java.util.Collection;

/**
 * Wrapper to wrap snippets with a code template.
 */
public class CodeGenWrapper extends Wrapper {
    private static final String TEMPLATE_FILE = "template-gen.bal";

    public CodeGenWrapper() {
        super(TEMPLATE_FILE);
    }

    @Override
    public String wrap(Collection<ImportSnippet> imports,
                       Collection<ModuleMemberDeclarationSnippet> moduleDeclarations,
                       Collection<VariableDefinitionSnippet> variableDeclarationSnippets,
                       Collection<StatementSnippet> statements, ExpressionSnippet expression) {
        String sourceCodeTemplate = readTemplate();
        return String.format(sourceCodeTemplate,
                toCodeLines(imports),
                toCodeLines(moduleDeclarations),
                toCodeLines(variableDeclarationSnippets),
                toCodeLines(statements),
                expression.toSourceCode());
    }

    /**
     * Helper method to convert snippets into lines of code.
     * Each source code of snippets will be joined by new line.
     *
     * @param snippets Snippets.
     * @return Joined source code.
     */
    private <T extends Snippet<?>> String toCodeLines(Collection<T> snippets) {
        StringBuilder generated = new StringBuilder();
        for (T snippet : snippets) {
            generated.append(snippet.toSourceCode()).append('\n');
        }
        return generated.toString();
    }
}
