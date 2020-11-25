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

package io.ballerina.shell.invoker.classload;

import java.util.HashMap;

/**
 * The static memory storage that the ballerina code will store values in.
 * This is a very basic hashmap that is only accessed by the client bal files.
 * Persists the global variables.
 * Uses a string id (context id) to keep different sessions.
 */
public class ClassLoadMemory {
    private static final HashMap<String, HashMap<String, Object>> memory = new HashMap<>();

    /**
     * Recalls the variable value.
     * This will return null if variable is not cached.
     *
     * @param contextId Context id.
     * @param name      Name of the variables.
     * @return The value of the variable.
     */
    @SuppressWarnings("unused")
    public static Object recall(String contextId, String name) {
        if (memory.containsKey(contextId)) {
            return memory.get(contextId).getOrDefault(name.trim(), null);
        }
        return null;
    }

    /**
     * Memorizes the variable value.
     *
     * @param contextId Context id.
     * @param name      Name of the variable.
     * @param value     Value of the variable.
     */
    @SuppressWarnings("unused")
    public static void memorize(String contextId, String name, Object value) {
        HashMap<String, Object> contextMem = memory.getOrDefault(contextId, new HashMap<>());
        contextMem.put(name.trim(), value);
        memory.put(contextId, contextMem);
    }

    /**
     * Clears memory.
     *
     * @param contextId Context id.
     */
    public static void forgetAll(String contextId) {
        if (memory.containsKey(contextId)) {
            memory.get(contextId).clear();
        }
    }
}
