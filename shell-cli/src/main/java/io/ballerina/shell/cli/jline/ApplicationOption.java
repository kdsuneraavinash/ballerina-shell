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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * CLI option enum with the name and description.
 */
public enum ApplicationOption {
    DEBUG('d', "debug", "start with debug mode", false),
    EXECUTOR('e', "executor", "executor to use", true),
    HELP('h', "help", "prints this help message", false);

    private final char code;
    private final String name;
    private final String description;
    private final boolean hasArg;

    ApplicationOption(char code, String name, String description, boolean hasArg) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.hasArg = hasArg;
    }

    /**
     * Creates an option that can be passed into a {@code CommandLine}.
     *
     * @return Option object.
     */
    public Option toOption() {
        Option option = new Option(String.valueOf(this.code), this.name, this.hasArg, this.description);
        option.setRequired(false);
        return option;
    }

    /**
     * Check whether the option is set in the given command line.
     *
     * @param cli Command line object.
     * @return Whether the option is set.
     */
    public boolean hasOptionSet(CommandLine cli) {
        assert !this.hasArg;
        return cli.hasOption(code);
    }

    /**
     * Get the value set for the given command line.
     *
     * @param cli          Command line object.
     * @param defaultValue Default value in case no value is set.
     * @return Option value.
     */
    @SuppressWarnings("unused")
    public String getOptionValue(CommandLine cli, String defaultValue) {
        assert this.hasArg;
        return cli.getOptionValue(code, defaultValue);
    }
}
