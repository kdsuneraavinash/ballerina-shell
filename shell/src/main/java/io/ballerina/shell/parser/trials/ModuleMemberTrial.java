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

import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.regex.Pattern;

/**
 * Attempts to capture a module member declaration.
 * Puts in the module level and checks for module level entries.
 * Checks if this is a possible module dcln. If it is definitely as module dcln,
 * any error is rejected. Otherwise, it is still checked.
 */
public class ModuleMemberTrial extends DualTreeParserTrial {
    public static final Pattern LISTENER_START = Pattern.compile("^listener[^\\w]");
    public static final Pattern SERVICE_START = Pattern.compile("^(isolated)?\\s+service[^\\w]");
    public static final Pattern FUNCTION_START = Pattern.compile("^(isolated)?\\s+function[^\\w]");
    public static final Pattern TYPE_START = Pattern.compile("^type[^\\w]");
    public static final Pattern CONST_START = Pattern.compile("^const[^\\w]");
    public static final Pattern ENUM_START = Pattern.compile("^enum[^\\w]");
    public static final Pattern XMLNS_START = Pattern.compile("^xmlns[^\\w]");
    public static final Pattern[] PATTERNS = {
            LISTENER_START, SERVICE_START, FUNCTION_START,
            TYPE_START, CONST_START, ENUM_START, XMLNS_START
    };

    @Override
    public Node parseSource(String source) throws ParserTrialFailedException {
        String trimmedSource = source.trim();
        boolean definitelyModuleDcln = false;
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(trimmedSource).matches()) {
                definitelyModuleDcln = true;
                break;
            }
        }

        try {
            TextDocument document = TextDocuments.from(source);
            SyntaxTree tree = SyntaxTree.from(document);
            assertTree(tree);

            ModulePartNode node = tree.rootNode();
            assertIf(!node.members().isEmpty(), "expected at least one member");

            ModuleMemberDeclarationNode dclnNode = node.members().get(0);

            // Only captured binding patterns can be global variables
            if (dclnNode instanceof ModuleVariableDeclarationNode) {
                assertIf(((ModuleVariableDeclarationNode) dclnNode).typedBindingPattern().bindingPattern()
                                instanceof CaptureBindingPatternNode,
                        "Only captured binding patterns can be global variables");
            }
            return dclnNode;
        } catch (ParserTrialFailedException e) {
            if (definitelyModuleDcln) {
                throw new ParserRejectedException(e.getMessage());
            }
            throw e;
        }
    }
}
