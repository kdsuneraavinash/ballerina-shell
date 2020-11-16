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

package io.ballerina.shell.executor.asm;

import io.ballerina.shell.executor.invoker.JarShellInvoker;
import io.ballerina.shell.executor.reeval.ReEvalExecutor;
import io.ballerina.shell.executor.reeval.ReEvalState;

/**
 * Executes the Jar file by building it first.
 * Works same as re-eval executor.
 */
public class JarExecutor extends ReEvalExecutor {
    private static final String TEMPLATE_FILE = "template.reeval.ftl";
    private static final String GENERATED_FILE = "main.bal";
    private static final String JAR_FILE = "main.jar";

    public JarExecutor() {
        super(TEMPLATE_FILE, new ReEvalState(), new JarShellInvoker(GENERATED_FILE, JAR_FILE));
    }
}
