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

package io.ballerina.shell.cli.jline;

import io.ballerina.shell.Evaluator;
import io.ballerina.shell.cli.Configuration;
import io.ballerina.shell.invoker.Invoker;
import io.ballerina.shell.invoker.replay.ReplayInvoker;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Configuration that uses command utils to provide options.
 */
public class ApplicationConfiguration extends Configuration {
    private static final Path BALLERINA_RUNTIME = Paths.get("home/bre/lib/*");
    private static final Path BALLERINA_HOME_PATH = Paths.get("home");
    private static final String TEMPLATE_FILE = "template.replay.ftl";
    private static final String TEMP_FILE_NAME = "main.bal";

    /**
     * Modes to create the evaluator.
     */
    public enum EvaluatorMode {
        REPLAY
    }

    public ApplicationConfiguration(boolean isDebug, EvaluatorMode mode) {
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
        if (mode == EvaluatorMode.REPLAY) {
            Invoker invoker = new ReplayInvoker(
                    TEMPLATE_FILE, TEMP_FILE_NAME,
                    BALLERINA_RUNTIME, BALLERINA_HOME_PATH);
            Evaluator evaluator = new Evaluator();
            evaluator.setPreprocessor(new SeparatorPreprocessor());
            evaluator.setTreeParser(new TrialTreeParser());
            evaluator.setSnippetFactory(new BasicSnippetFactory());
            evaluator.setInvoker(invoker);
            return evaluator;
        }
        throw new RuntimeException("Unknown mode given.");
    }
}
