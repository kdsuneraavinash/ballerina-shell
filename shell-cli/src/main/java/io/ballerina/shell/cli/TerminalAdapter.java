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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A library independent adapter for ballerina shell.
 */
public abstract class TerminalAdapter {
    @SuppressWarnings("unused")
    protected static final int BLACK = 0;
    protected static final int RED = 1;
    protected static final int GREEN = 2;
    protected static final int YELLOW = 3;
    protected static final int BLUE = 4;
    @SuppressWarnings("unused")
    protected static final int MAGENTA = 5;
    protected static final int CYAN = 6;
    @SuppressWarnings("unused")
    protected static final int WHITE = 7;
    protected static final int BRIGHT = 8;

    /**
     * Returns the writer object to write.
     *
     * @return Writer to write.
     */
    protected abstract PrintWriter writer();

    /**
     * Colors a text by the given color.
     *
     * @param text  String to color.
     * @param color Color to use.
     * @return Colored string.
     */
    protected abstract String color(String text, int color);

    /**
     * Reads a line of input from user.
     *
     * @param prefix  Prefix of the input prompt.
     * @param postfix Postfix of the input prompt.
     * @return Input string.
     */
    public abstract String readLine(String prefix, String postfix);

    public void result(String text) {
        this.println(this.color(text, BLUE));
    }

    public void error(String text) {
        this.println(this.color(indented(text), RED | BRIGHT));
    }

    public void warn(String text) {
        this.println(this.color(indented(text), YELLOW));
    }

    public void debug(String text) {
        this.println(this.color(indented(text), BRIGHT));
    }

    public void fatalError(String text) {
        this.println(this.color(indented(text), RED));
    }

    public void info(String text) {
        this.println(this.color(indented(text), CYAN));
    }

    public void println(String text) {
        this.writer().println(text);
        this.writer().flush();
    }

    private String indented(String text) {
        // All info data is appended with |, similar to jshell
        return Arrays.stream(text.split("\n")).map(s -> "|  " + s)
                .collect(Collectors.joining("\n"));
    }
}
