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

import io.ballerina.shell.utils.debug.DebugWriter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Diagnostic writer that will write to the
 * REPL with a different color output.
 */
public class ReplDebugWriter implements DebugWriter {
    private final Terminal terminal;

    public ReplDebugWriter(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void write(String write) {
        String message = new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT))
                .append(write).toAnsi();
        terminal.writer().println(message);
        terminal.writer().flush();
    }
}