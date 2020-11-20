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

/**
 * A library independent adapter for ballerina shell.
 */
public interface TerminalAdapter {
    int BLACK = 0;
    int RED = 1;
    int GREEN = 2;
    int YELLOW = 3;
    int BLUE = 4;
    int MAGENTA = 5;
    int CYAN = 6;
    int WHITE = 7;
    int BRIGHT = 8;

    /**
     * Adapter exception for user exit exceptions.
     */
    class UserException extends Exception {
    }

    /**
     * Returns the writer object to write.
     *
     * @return Writer to write.
     */
    PrintWriter writer();

    /**
     * Colors a text by the given color.
     *
     * @param text  String to color.
     * @param color Color to use.
     * @return Colored string.
     */
    String color(String text, int color);

    /**
     * Reads a line of input from user.
     *
     * @param prefix  Prefix of the input prompt.
     * @param postfix Postfix of the input prompt.
     * @return Input string.
     */
    String readLine(String prefix, String postfix);
}
