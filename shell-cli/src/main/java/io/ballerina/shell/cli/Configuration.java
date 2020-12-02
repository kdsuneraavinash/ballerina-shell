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

package io.ballerina.shell.cli;

import io.ballerina.shell.Evaluator;
import io.ballerina.shell.EvaluatorBuilder;

import java.util.Objects;

/**
 * Configuration that uses command utils to provide options.
 */
public class Configuration {
    protected boolean isDebug;
    protected Evaluator evaluator;

    /**
     * Modes to create the evaluator.
     */
    public enum EvaluatorMode {
        DEFAULT
    }

    public Configuration(boolean isDebug, EvaluatorMode mode) {
        Objects.requireNonNull(mode, "Mode is a required parameter.");
        this.isDebug = isDebug;
        this.evaluator = createEvaluator(mode);
    }

    /**
     * Creates and returns an evaluator based on the config.
     *
     * @param mode Mode to create the evaluator on.
     * @return Created evaluator.
     */
    private Evaluator createEvaluator(EvaluatorMode mode) {
        if (mode == EvaluatorMode.DEFAULT) {
            return new EvaluatorBuilder().build();
        }
        throw new RuntimeException("Unknown mode given.");
    }

    /**
     * Whether the configuration is in debug mode.
     *
     * @return Whether in debug mode.
     */
    public boolean isDebug() {
        return isDebug;
    }

    /**
     * Get the evaluator set by the user.
     *
     * @return Evaluator.
     */
    public Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * Toggles debug mode.
     */
    public void toggleDebug() {
        isDebug = !isDebug;
    }
}
