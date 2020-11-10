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

/**
 * Generic transformer interface to transform one type to another.
 * Used to transform the Syntax Tree before sending to the wrapper as snippets.
 *
 * @param <P> Transformer input type.
 * @param <Q> Transformer output type.
 */
public interface Transformer<P, Q> {
    /**
     * Transforms a value to another type.
     *
     * @param value Input value.
     * @return Transformed value.
     */
    Q transform(P value);
}
