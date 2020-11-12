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
package org.ballerina.repl;


/**
 * CLI option enum with the name and description.
 */
enum ReplCommandOption {
    DEBUG("debug", "start with debug mode)", false),
    EXECUTOR("executor", "executor to use (gen/reeval)", true),
    HELP("help", "prints this help message", false);

    final String name;
    final String description;
    final boolean hasArg;

    ReplCommandOption(String name, String description, boolean hasArg) {
        this.name = name;
        this.description = description;
        this.hasArg = hasArg;
    }

    /**
     * The option parameter.
     * This is the first letter of the name.
     */
    String option() {
        return name.substring(0, 1);
    }
}
