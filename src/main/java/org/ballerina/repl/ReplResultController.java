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

import io.ballerina.shell.LogStatus;
import io.ballerina.shell.ShellController;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Shell controller that will manage outputting the shell
 * output to the terminal.
 * Colors will be used as necessary.
 */
public class ReplResultController implements ShellController {
    private final Terminal terminal;

    public ReplResultController(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void startSession() {
        // Nothing
    }

    @Override
    public void emitResult(String output, LogStatus status) {
        if (status != LogStatus.SUCCESS) {
            output = new AttributedStringBuilder()
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
                    .append(output).toAnsi();
        }
        terminal.writer().println(output);
        terminal.writer().flush();
    }

    @Override
    public void finishSession() {
    }

    @Override
    public void failSession() {
    }
}
