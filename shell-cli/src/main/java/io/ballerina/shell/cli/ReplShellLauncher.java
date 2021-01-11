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

import picocli.CommandLine;

import java.util.concurrent.Callable;


/**
 * Ballerina base shell REPL.
 * Executes a interactive shell to let the user interact with Ballerina Shell.
 */
@CommandLine.Command(name = "ballerina shell", mixinStandardHelpOptions = true, version = "ballerina shell 0.01",
        description = "Shell program for Ballerina.")
public class ReplShellLauncher implements Callable<Integer> {
    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = {"-m", "--mode"}, description = "Mode to operate the REPL.",
            type = BShellConfiguration.EvaluatorMode.class)
    private BShellConfiguration.EvaluatorMode mode = BShellConfiguration.EvaluatorMode.DEFAULT;

    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = {"-d", "--debug"}, description = "Whether to enable debug mode from start.")
    private boolean isDebug = false;

    @SuppressWarnings("FieldMayBeFinal")
    @CommandLine.Option(names = {"-f", "--force-dumb"}, description = "Whether to force use of dumb terminal.")
    private boolean forceDumb = false;

    @CommandLine.Option(names = {"-t", "--time-out"}, description = "Timeout to use for tree parsing.")
    private long timeOut = 1000;

    /**
     * Launch the REPL.
     *
     * @param args Optional arguments.
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new ReplShellLauncher())
                .setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        BShellConfiguration configuration = new BShellConfiguration.Builder()
                .setDebug(isDebug).setDumb(forceDumb)
                .setTreeParsingTimeoutMs(timeOut).setEvaluatorMode(mode).build();
        ReplShellApplication.execute(configuration);
        return 0;
    }
}
