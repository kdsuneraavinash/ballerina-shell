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

/**
 * State is the object where the executor persists
 * its execution state.
 */
public interface State {
    /**
     * Reset state to initial state.
     */
    void reset();

    /**
     * Adds a snippet to state on the correct array depending on the
     * type of new snippet.
     * For example, if the new snippet is a expression, that list would be operated.
     *
     * @param newSnippet Snippet to check the type of.
     */
    void addSnippet(Snippet newSnippet);
}
