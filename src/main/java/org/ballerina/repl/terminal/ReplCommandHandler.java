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

import io.ballerina.shell.utils.debug.DebugProvider;
import org.ballerina.repl.ReplShell;
import org.ballerina.repl.exceptions.ReplExitException;
import org.ballerina.repl.exceptions.ReplHandledException;
import org.ballerina.repl.exceptions.ReplResetException;
import org.ballerina.repl.exceptions.ReplToggleDebugException;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.PrintWriter;

/**
 * Handler to handle built-in REPL commands.
 * Exit, Help, etc... commands are invoked in this manner.
 * This will throw an Exception if handled.
 */
public class ReplCommandHandler {
    private static final String ABOUT_FILE = "about.txt";
    private static final String IMPORTS_FILE = "imports.txt";
    private static final String EXIT_COMMAND = "/exit";
    private static final String ABOUT_COMMAND = "/about";
    private static final String TOGGLE_DEBUG = "/debug";
    private static final String RESET_COMMAND = "/reset";
    private static final String IMPORTS_COMMAND = "/imports";
    private static final String EMPTY_LINE = "";

    private static String coloredText(String text) {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT))
                .append(text)
                .toAnsi();
    }

    /**
     * This would handle the command and if identified as a valid command,
     * would execute something and throw a exception to signify that.
     * This would also throw exceptions to signify to exit.
     * If no exception, then that signifies that the handler didn't identify the command.
     *
     * @param command Command to parse.
     * @param output  Output writer to send output to.
     * @throws ReplExitException        to signify to exit the program.
     * @throws ReplHandledException     to signify that the command was handled.
     * @throws ReplToggleDebugException to signify to change the debug mode.
     */
    public static void handle(String command, PrintWriter output)
            throws ReplExitException, ReplHandledException, ReplToggleDebugException, ReplResetException {
        switch (command.toLowerCase().trim()) {
            case EXIT_COMMAND:
                throw new ReplExitException();
            case ABOUT_COMMAND:
                String aboutContent = ReplShell.readFile(ABOUT_FILE);
                output.println(coloredText(aboutContent));
                throw new ReplHandledException();
            case IMPORTS_COMMAND:
                String importsContent = ReplShell.readFile(IMPORTS_FILE);
                output.println(coloredText(importsContent));
                throw new ReplHandledException();
            case RESET_COMMAND:
                output.println(coloredText("Resetting State"));
                throw new ReplResetException();
            case EMPTY_LINE:
                throw new ReplHandledException();
            case TOGGLE_DEBUG:
                throw new ReplToggleDebugException();
            default:
                DebugProvider.sendMessage("Command not identified as internal command.");
        }
    }
}
