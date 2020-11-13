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

import java.util.HashMap;
import java.util.Map;

/**
 * Executor state for source gen executor.
 */
public class SourceGenExecutorState {
    private final Map<String, String> vars;

    public SourceGenExecutorState() {
        vars = new HashMap<>();
    }

    /**
     * Names of the variables that are preserved in state.
     *
     * @return Variable names.
     */
    public Iterable<String> allVariables() {
        return vars.keySet();
    }

    /**
     * Value of a variable given.
     *
     * @param variableName Name of the var to check.
     * @return Value of the variable.
     */
    public String valueOfVariable(String variableName) {
        return vars.get(variableName);
    }

    @Override
    public String toString() {
        return String.format("(State) vars = %s.", vars.size());
    }
}
