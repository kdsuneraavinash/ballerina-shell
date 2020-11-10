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

import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;

/**
 * Module level declarations.
 * These are not active or runnable except for service declarations.
 * Any undefined variable in these declarations are ignored.
 * (Except for module level variable declarations)
 */
public class ModuleMemberDeclarationSnippet extends Snippet<ModuleMemberDeclarationNode> {
    public ModuleMemberDeclarationSnippet(ModuleMemberDeclarationNode node) {
        super(node, SnippetKind.MODULE_MEMBER_DECLARATION_KIND);
    }
}
