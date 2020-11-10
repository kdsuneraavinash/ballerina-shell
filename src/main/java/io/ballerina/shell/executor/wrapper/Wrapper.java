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
package io.ballerina.shell.executor.wrapper;

import io.ballerina.shell.snippet.Snippet;

import java.util.Collection;

/**
 * Wraps the snippets in a code segment and
 * generate the corresponding syntax tree.
 */
public interface Wrapper {
    /**
     * Wraps the snippets.
     *
     * @param imports            Import statement snippets.
     * @param moduleDeclarations Module level declaration snippets.
     * @param statements         Statement snippets to wrapped in a function body.
     * @param expression         Expression statement that is evaluated.
     * @return Source code corresponding to the wrapped snippets.
     */
    String wrap(Collection<Snippet<?>> imports,
                Collection<Snippet<?>> moduleDeclarations,
                Collection<Snippet<?>> statements, Snippet<?> expression);
}
