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
package io.ballerina.shell.transformer;

import io.ballerina.shell.snippet.Snippet;

/**
 * Generic transformer interface to transform one snippet to another.
 * Maybe used to decorate snippets, or remove snippet decorations.
 */
public interface Transformer {
    /**
     * Transforms a snippet to another snippet.
     *
     * @param snippet Input snippet.
     * @return Transformed snippet.
     */
    Snippet transform(Snippet snippet);
}
