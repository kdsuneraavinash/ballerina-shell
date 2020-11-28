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

package io.ballerina.shell.utils;

/**
 * A simple immutable data structure
 * that holds a pair of values.
 *
 * @param <P> The type of first value.
 * @param <Q> The type of second value.
 */
public class Pair<P, Q> {
    private final P first;
    private final Q second;

    public Pair(P first, Q second) {
        this.first = first;
        this.second = second;
    }

    public P getFirst() {
        return first;
    }

    public Q getSecond() {
        return second;
    }
}
