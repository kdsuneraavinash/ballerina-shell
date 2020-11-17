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

package io.ballerina.shell.snippet.types;

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.SnippetSubKind;

/**
 * Snippet that represent a import statement.
 * TODO: How to defer unused imports until they are used?
 */
public class ImportSnippet extends Snippet {
    private final String importName;

    protected ImportSnippet(Node node, SnippetSubKind subKind, String importName) {
        super(node.toSourceCode(), subKind);
        this.importName = importName;
        assert subKind.getKind() == SnippetKind.IMPORT_KIND;
    }

    /**
     * Create a import snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ImportSnippet tryFromNode(Node node) {
        if (node instanceof ImportDeclarationNode) {
            ImportDeclarationNode importNode = ((ImportDeclarationNode) node);
            if (importNode.prefix().isEmpty()) {
                return new ImportSnippet(node, SnippetSubKind.IMPORT, null);
            }
            String importPrefix = importNode.prefix().get().prefix().text();
            return new ImportSnippet(node, SnippetSubKind.IMPORT_WITH_PREFIX, importPrefix);
        }
        return null;
    }


    @SuppressWarnings("unused")
    public String getImportName() {
        return importName;
    }
}
