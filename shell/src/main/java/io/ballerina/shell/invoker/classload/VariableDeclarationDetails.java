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

package io.ballerina.shell.invoker.classload;

import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.Pair;

/**
 * Extracts the variable details.
 */
public class VariableDeclarationDetails {
    private final String name;
    private final String type;

    public VariableDeclarationDetails(VariableDeclarationSnippet snippet) throws SnippetException {
        ModuleVariableDeclarationNode node = snippet.getRootNode();

        // Validate expected global variable format
        if (node.metadata().isPresent() || !node.qualifiers().isEmpty()
                || !(node.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode)) {
            throw new SnippetException();
        }

        this.name = node.typedBindingPattern().bindingPattern().toSourceCode().trim();
        this.type = node.typedBindingPattern().typeDescriptor().toSourceCode().trim();
    }

    /**
     * Get the name and type as a pair.
     * The first value will be the name.
     * Second value is the type.
     *
     * @return The pair of values.
     */
    public Pair<String, String> varNameAndType() {
        return new Pair<>(name, type);
    }
}
