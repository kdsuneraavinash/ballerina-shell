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

package org.ballerina.repl.terminal;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handler to handle built-in REPL commands.
 * Exit, Help, etc... commands are invoked in this manner.
 * This will throw an Exception if handled.
 */
public class ReplCommandHandler {
    private static final String COMMAND_PREFIX = "/";

    private final Map<String, Consumer<PrintWriter>> commandHandlers;

    public ReplCommandHandler() {
        this.commandHandlers = new HashMap<>();
    }

    /**
     * Attach a command handler with a command.
     * The command should not contain the command prefix.
     * Prefix would be added by this method.
     *
     * @param command Command to register. (without prefix)
     * @param handler Handler to use.
     */
    public void attachHandler(String command, Consumer<PrintWriter> handler) {
        commandHandlers.put(COMMAND_PREFIX + command, handler);
    }

    /**
     * This would handle the command and if identified as a valid command,
     * would execute the handler attached to it.
     *
     * @param output  Output writer to send output to.
     * @param command Command to parse.
     * @return whether the command was handled.
     */
    public boolean handle(String command, PrintWriter output) {
        command = command.trim();
        if (commandHandlers.containsKey(command)) {
            commandHandlers.get(command).accept(output);
            return true;
        }
        return false;
    }
}
