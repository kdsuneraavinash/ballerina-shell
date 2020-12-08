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

package io.ballerina.shell.parser.trials;

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

/**
 * Attempts to parse source as a import statement.
 * Puts in the module level and checks for the import entry.
 * Doesn't have any false positives or true negatives.
 * Only checked if the source starts with import keyword.
 */
public class ImportDeclarationTrial extends TreeParserTrial {
    public ImportDeclarationTrial(TrialTreeParser parentParser) {
        super(parentParser);
    }

    @Override
    public Node parse(String source) throws ParserTrialFailedException {
        assertIf(source.trim().startsWith("import "), "expected to start with 'import'");

        try {
            TextDocument document = TextDocuments.from(source);
            SyntaxTree tree = getSyntaxTree(document);

            ModulePartNode modulePartNode = tree.rootNode();
            NodeList<ImportDeclarationNode> imports = modulePartNode.imports();
            assertIf(!imports.isEmpty(), "expected import member");
            return imports.get(0);
        } catch (ParserTrialFailedException e) {
            throw new ParserRejectedException(e.getMessage());
        }
    }
}
