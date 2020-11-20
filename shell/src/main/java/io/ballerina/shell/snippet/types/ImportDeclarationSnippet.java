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
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

/**
 * Snippet that represent a import statement.
 * TODO: How to defer unused imports until they are used?
 * TODO: Other imports are not supported because BRE is not supported.
 */
public class ImportDeclarationSnippet extends Snippet {
    public ImportDeclarationSnippet(ImportDeclarationNode rootNode) {
        super(SnippetKind.IMPORT_DECLARATION, rootNode, false);
    }
}