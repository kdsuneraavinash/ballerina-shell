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

package io.ballerina.shell.executor.dynamic;

import io.ballerina.shell.executor.Context;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.executor.dynamic.invoker.DynamicInvoker;
import io.ballerina.shell.executor.dynamic.invoker.DynamicShellInvoker;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.io.IOException;
import java.util.List;

/**
 * Executes the snippet given.
 * Re evaluates the snippet by generating a file containing all snippets
 * and executing it.
 */
public class DynamicExecutor extends Executor<DynamicState, DynamicContext, DynamicInvoker> {
    private static final String TEMPLATE_FILE = "template.dynamic.mustache";
    private static final String GENERATED_FILE = "main.bal";

    public DynamicExecutor() {
        super(TEMPLATE_FILE, new DynamicState(), new DynamicShellInvoker(GENERATED_FILE));
    }

    @Override
    public DynamicContext currentContext(Snippet newSnippet) {
        List<String> imports = Context.snippetsToStrings(state.imports());
        List<String> moduleDeclarations = Context.snippetsToStrings(state.moduleDeclarations());
        List<String> variableDefinitions = Context.snippetsToStrings(state.variableDefinitions());

        if (newSnippet.getKind() == SnippetKind.IMPORT_KIND) {
            imports.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND) {
            moduleDeclarations.add(newSnippet.toSourceCode());
        } else if (newSnippet.getKind() == SnippetKind.VARIABLE_DEFINITION_KIND) {
            variableDefinitions.add(newSnippet.toSourceCode());
        }

        return new DynamicContext(imports, moduleDeclarations, variableDefinitions, newSnippet);
    }

    @Override
    public boolean executeInvoker(Postprocessor postprocessor)
            throws InterruptedException, IOException, ClassNotFoundException {
        return invoker.execute(state, postprocessor);
    }
}
