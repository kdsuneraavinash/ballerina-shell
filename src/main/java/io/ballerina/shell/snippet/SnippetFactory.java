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

package io.ballerina.shell.snippet;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.util.List;
import java.util.function.Supplier;

/**
 * A utility class to create snippets from nodes.
 */
public class SnippetFactory {
    /**
     * Creates a snippet from the given node.
     * This will throw and error if the resultant snippet is an erroneous snippet.
     */
    public static Snippet fromNode(Node node) {
        List<Supplier<Snippet>> suppliers = List.of(
                () -> ImportSnippet.tryFromNode(node),
                () -> VariableDefinitionSnippet.tryFromNode(node),
                () -> ModuleMemberDeclarationSnippet.tryFromNode(node),
                () -> ExpressionSnippet.tryFromNode(node),
                () -> StatementSnippet.tryFromNode(node),
                () -> ErroneousSnippet.tryFromNode(node)
        );
        Snippet snippet;
        for (Supplier<Snippet> supplier : suppliers) {
            snippet = supplier.get();
            if (snippet != null && snippet.isValid()) {
                // If snippet creation failed or
                // snippet is handled by another type
                return snippet;
            }
        }

        // Should not have reached here. But whatever.
        DebugProvider.sendMessage("Snippet parsing failed.");
        throw new SnippetException("Invalid syntax. Unknown snippet type.");
    }
}
