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

import io.ballerina.shell.cli.Configuration;
import io.ballerina.shell.cli.HelpException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Objects;

/**
 * Configuration that uses command utils to provide options.
 */
public class ApplicationConfiguration extends Configuration {
    public ApplicationConfiguration(CommandLine cmd) throws HelpException {
        Objects.requireNonNull(cmd, "Command line arguments were not received.");
        if (ApplicationOption.HELP.hasOptionSet(cmd)) {
            throw new HelpException();
        }
        isDebug = ApplicationOption.DEBUG.hasOptionSet(cmd);
        evaluator = EvaluatorType.REPLAY;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public EvaluatorType getEvaluator() {
        return evaluator;
    }

    public void toggleDebug() {
        isDebug = !isDebug;
    }

    /**
     * Generate the CLI options.
     * These options will be used by the CLI parser to
     * get the necessary configurations.
     *
     * @return Generated CLI options.
     */
    public static Options getConfigurationOptions() {
        Options options = new Options();
        for (ApplicationOption op : ApplicationOption.values()) {
            Option option = op.toOption();
            options.addOption(option);
        }
        return options;
    }
}
