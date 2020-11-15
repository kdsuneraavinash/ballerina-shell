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

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.Objects;

/**
 * Attempts to parse source as a import statement.
 * Puts in the module level and checks for the import entry.
 * Doesn't have any false positives or true negatives.
 */
public class ImportDeclarationTrial implements TreeParserTrial {
    @Override
    public Node tryParse(String source) throws ParserTrialFailedException {
        try {
            TextDocument document = TextDocuments.from(source);
            SyntaxTree tree = SyntaxTree.from(document);
            ModulePartNode node = tree.rootNode();

            ImportDeclarationNode importDeclarationNode = node.imports().get(0);
            Objects.requireNonNull(importDeclarationNode);
            return importDeclarationNode;
        } catch (Exception e) {
            throw new ParserTrialFailedException(e);
        }
    }
}
