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

package io.ballerina.shell.treeparser;

import io.ballerina.compiler.syntax.tree.Node;

/**
 * In this stage the correct syntax tree is identified.
 * The root node of the syntax tree must be the corresponding
 * type for the statement.
 * For an example, for a import declaration,
 * the tree that is parsed should have
 * {@code ImportDeclarationNode} as the root node.
 */
public interface TreeParser {
    /**
     * Parses a source code string into a Node.
     * Input source code is expected to be a single statement/expression.
     *
     * @param source Input source code statement.
     * @return Syntax tree for the source code.
     */
    Node parse(String source);
}
