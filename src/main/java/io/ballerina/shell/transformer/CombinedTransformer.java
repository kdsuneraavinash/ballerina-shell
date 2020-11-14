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
import io.ballerina.shell.utils.debug.DebugProvider;

/**
 * A transformer to apply all transformers.
 */
public class CombinedTransformer implements Transformer {
    private final Transformer[] transformers;

    /**
     * Create a combined transformer by the given transformers.
     *
     * @param transformers All the transformers to combine.
     */
    public CombinedTransformer(Transformer... transformers) {
        // Send a debug message of transformers
        DebugProvider.sendMessage("Attached %s transformers.",
                String.valueOf(transformers.length));

        this.transformers = transformers;
    }

    @Override
    public Snippet transform(Snippet snippet) {
        for (Transformer transformer : transformers) {
            snippet = transformer.transform(snippet);
        }
        return snippet;
    }
}
