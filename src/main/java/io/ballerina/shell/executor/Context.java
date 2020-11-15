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

package io.ballerina.shell.executor;

import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.util.ArrayList;
import java.util.List;

/**
 * A Tag to contexts for the mustache generation.
 * Provides helper methods to populate fields.
 */
public abstract class Context {
    protected final String newStatement;
    protected final String newExpression;

    protected Context(Snippet newSnippet) {
        // Get the new statement/new expression - if any
        // New statement/new expression will be set depending on the type
        String newExpression = null;
        String newStatement = null;
        if (newSnippet != null) {
            if (newSnippet.getKind() == SnippetKind.EXPRESSION_KIND) {
                newExpression = newSnippet.toSourceCode();
            } else if (newSnippet.getKind() == SnippetKind.STATEMENT_KIND) {
                newStatement = newSnippet.toSourceCode();
            }
        }
        this.newStatement = newStatement;
        this.newExpression = newExpression;
    }

    public static List<String> snippetsToStrings(Iterable<Snippet> snippets) {
        List<String> strings = new ArrayList<>();
        for (Snippet snippet : snippets) {
            strings.add(snippet.toSourceCode());
        }
        return strings;
    }
}
