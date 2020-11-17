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

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.TableConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.SnippetSubKind;

/**
 * These are expressions that are executable but are not persistent.
 * (Does not affect other statements/expressions)
 * These do not have to contain semicolons.
 * (If the expression is a Expression Statement, the semicolon will be stripped.)
 */
public class ExpressionSnippet extends Snippet {
    protected ExpressionSnippet(Node node, SnippetSubKind subKind) {
        super(node.toSourceCode(), subKind);
        assert subKind.getKind() == SnippetKind.EXPRESSION_KIND;
    }

    /**
     * Create a expression snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ExpressionSnippet tryFromNode(Node node) {
        if (node instanceof TypeTestExpressionNode) {
            return new ExpressionSnippet(node, SnippetSubKind.TYPE_TEST_EXPRESSION);
        } else if (node instanceof TableConstructorExpressionNode) {
            return new ExpressionSnippet(node, SnippetSubKind.TABLE_CONSTRUCTOR_EXPRESSION);
        } else if (node instanceof ServiceConstructorExpressionNode) {
            return new ExpressionSnippet(node, SnippetSubKind.SERVICE_CONSTRUCTOR_EXPRESSION);
        } else if (node instanceof ExpressionNode) {
            return new ExpressionSnippet(node, SnippetSubKind.OTHER_EXPRESSION);
        }
        return null;
    }
}
