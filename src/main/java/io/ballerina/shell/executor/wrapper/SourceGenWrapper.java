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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.ballerina.shell.executor.desugar.Binding;
import io.ballerina.shell.executor.desugar.BindingDesugar;
import io.ballerina.shell.snippet.ExpressionSnippet;
import io.ballerina.shell.snippet.ImportSnippet;
import io.ballerina.shell.snippet.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.StatementSnippet;
import io.ballerina.shell.snippet.VariableDefinitionSnippet;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper to wrap snippets with a code template.
 */
public class SourceGenWrapper extends Wrapper {
    private static final String TEMPLATE_FILE = "template.hbs.bal";
    private final BindingDesugar bindingDesugar;
    private final Mustache mustache;

    public SourceGenWrapper() {
        super(TEMPLATE_FILE);
        MustacheFactory mf = new DefaultMustacheFactory();
        mustache = mf.compile(TEMPLATE_FILE);
        bindingDesugar = new BindingDesugar();
    }

    @Override
    public String wrap(Collection<ImportSnippet> imports,
                       Collection<ModuleMemberDeclarationSnippet> moduleDeclarations,
                       Collection<VariableDefinitionSnippet> variableDeclarationSnippets,
                       Collection<StatementSnippet> statements, ExpressionSnippet expression) {

        // Create bindings and the context
        List<Binding> bindings = new ArrayList<>();
        for (VariableDefinitionSnippet snippet : variableDeclarationSnippets) {
            bindings.addAll(bindingDesugar.desugar(snippet));
        }
        SourceGenContext context = new SourceGenContext();
        for (Binding binding : bindings) {
            context.addVariable(binding.getIdentifierName());
        }
        ShellDiagnosticProvider.sendMessage("Found %s variable bindings.", String.valueOf(bindings.size()));

        context.imports = toCodeLines(imports);
        context.moduleDeclarations = toCodeLines(moduleDeclarations);
        context.variableDeclarationSnippets = toCodeLines(variableDeclarationSnippets);
        context.statements = toCodeLines(statements);
        context.expression = expression.toSourceCode();

        StringWriter writer = new StringWriter();
        try {
            mustache.execute(writer, context).flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return writer.toString();
    }

    /**
     * Helper method to convert snippets into lines of code.
     * Each source code of snippets will be a item in a list.
     *
     * @param snippets Snippets.
     * @return Joined source code as a list.
     */
    private <T extends Snippet> List<String> toCodeLines(Collection<T> snippets) {
        List<String> generated = new ArrayList<>();
        for (T snippet : snippets) {
            generated.add(snippet.toSourceCode());
        }
        return generated;
    }
}
