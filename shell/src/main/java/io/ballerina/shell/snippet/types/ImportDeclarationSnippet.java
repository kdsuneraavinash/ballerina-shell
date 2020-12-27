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

import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.StringUtils;

import java.util.stream.Collectors;

/**
 * Snippet that represent a import statement.
 */
public class ImportDeclarationSnippet extends Snippet {
    public ImportDeclarationSnippet(ImportDeclarationNode rootNode) {
        super(SnippetSubKind.IMPORT_DECLARATION, rootNode);
    }

    /**
     * Finds the import alias used for the import statement.
     * Eg: alias of 'import abc/pqr' is 'pqr'
     * This is prefix or the last module name.
     *
     * @return Alias of this import.
     */
    public String getPrefix() {
        ImportDeclarationNode importNode = (ImportDeclarationNode) rootNode;
        return importNode.prefix().isPresent()
                ? importNode.prefix().get().prefix().text()
                : importNode.moduleName().get(importNode.moduleName().size() - 1).text();
    }

    /**
     * Finds the import expression or the imported module.
     * This will follow `orgName/module1.module2` format.
     *
     * @return Imported module expression.
     */
    public String getImportedModule() {
        ImportDeclarationNode importNode = (ImportDeclarationNode) rootNode;
        String moduleName = importNode.moduleName().stream()
                .map(IdentifierToken::text)
                .map(StringUtils::quoted)
                .collect(Collectors.joining("."));
        if (importNode.orgName().isPresent()) {
            String orgName = StringUtils.quoted(importNode.orgName().get().orgName().text());
            return String.format("%s/%s", orgName, moduleName);
        }
        return moduleName;
    }
}
