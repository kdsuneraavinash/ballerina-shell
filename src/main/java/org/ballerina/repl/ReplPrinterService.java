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
import io.ballerina.shell.PrinterService;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;

/**
 * Diagnostic writer that will write to the
 * REPL with a different color output.
 */
public class ReplPrinterService implements PrinterService {
    private final Terminal terminal;

    public ReplPrinterService(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void write(String output, LogStatus status) {
        int color;
        switch (status) {
            case FATAL_ERROR:
                color = AttributedStyle.RED;
                break;
            case ERROR:
                color = AttributedStyle.YELLOW;
                break;
            case WARNING:
                color = AttributedStyle.CYAN;
                break;
            case SUCCESS:
                color = AttributedStyle.GREEN;
                break;
            default:
                color = AttributedStyle.BRIGHT;
                break;
        }
        output = ReplShell.colored(output, color);
        terminal.writer().println(output);
        terminal.flush();
    }
}
