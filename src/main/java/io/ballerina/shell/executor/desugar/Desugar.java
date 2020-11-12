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
package io.ballerina.shell.executor.desugar;

import io.ballerina.shell.snippet.Snippet;

import java.util.List;

/**
 * Desugar process will normalize a snippet into a
 * list of (same type or different type) snippets/type.
 *
 * @param <P> Snippet type that will get desugared.
 * @param <Q> Converted type.
 */
public interface Desugar<P extends Snippet<?>, Q> {
    /**
     * Converts a snippets to a list of snippets.
     * The resultant snippets should be effectively same as the input.
     *
     * @param snippet Snippet to convert.
     * @return List of resultant objects.
     */
    List<Q> desugar(P snippet);
}
