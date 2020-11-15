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

package io.ballerina.shell.postprocessor;

import io.ballerina.shell.ShellController;

/**
 * Processes a string output to a processed string.
 * May perform some filtering, mapping on the output string.
 * Used to listen to STDOUT, STDERR streams.
 */
public abstract class Postprocessor {
    public static final String ACTIVATION_START = "[[__START__]]";
    public static final String ACTIVATION_END = "[[__END__]]";

    protected final ShellController controller;

    public Postprocessor(ShellController controller) {
        this.controller = controller;
    }

    /**
     * Processes a line sent the stdout of the program.
     * This is the output that the user wanted to be printed.
     *
     * @param line Input string line.
     */
    public abstract void onProgramOutput(String line);

    /**
     * Processes a line sent by the compiler.
     * This is the STDERR, so could also be output sent by the program.
     * Generally these lines are compiler generated.
     *
     * @param line Input string line.
     */
    public abstract void onCompilerOutput(String line);
}
