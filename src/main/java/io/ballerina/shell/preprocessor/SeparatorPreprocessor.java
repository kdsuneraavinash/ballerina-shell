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
package io.ballerina.shell.preprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Preprocessor to split the input into several statements
 * based on the semicolons and brackets.
 * Outputs the split input.
 */
public class SeparatorPreprocessor implements Preprocessor {
    private static final char ESCAPE_CHAR = '\\';
    private static final char SINGLE_QUOTE = '\'';
    private static final char DOUBLE_QUOTE = '\"';
    private static final char SEMICOLON = ';';
    private static final char PARENTHESIS_OPEN = '(';
    private static final char PARENTHESIS_CLOSE = ')';
    private static final char SQUARE_BR_OPEN = '[';
    private static final char SQUARE_BR_CLOSE = ']';
    private static final char CURLY_BR_OPEN = '{';
    private static final char CURLY_BR_CLOSE = '}';
    private static final char ANGLE_BR_OPEN = '<';
    private static final char ANGLE_BR_CLOSE = '>';

    @Override
    public List<String> preprocess(String input) {
        List<String> snippets = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        Stack<Character> brackets = new Stack<>();

        boolean isInStringLiteral = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            builder.append(character);

            if (character == SINGLE_QUOTE || character == DOUBLE_QUOTE) {
                if (!isEscaped(input, i)) {
                    isInStringLiteral = !isInStringLiteral;
                }
            }

            if (!isInStringLiteral) {
                if (character == SEMICOLON && brackets.isEmpty()) {
                    snippets.add(builder.toString());
                    builder.setLength(0);
                } else if (isOpeningBracket(character)) {
                    brackets.push(character);
                } else if (!brackets.isEmpty() && isBracketPair(brackets.peek(), character)) {
                    brackets.pop();
                }
            }
        }

        if (!brackets.isEmpty()) {
            throw new RuntimeException("Brackets do not match");
        }

        // Append remaining string to the statements.
        // If there is a remainder then there wasn't a semicolon at end.
        // So, add missing semicolon as well.
        if (builder.length() > 0) {
            builder.append(SEMICOLON);
            snippets.add(builder.toString());
        }

        return snippets;
    }

    /**
     * Whether the character at the given position was escaped or not.
     *
     * @param input    Whole input string.
     * @param position Position of character to check.
     * @return Whether the character was escaped.
     */
    private boolean isEscaped(String input, int position) {
        return position > 0 && input.charAt(position - 1) == ESCAPE_CHAR;
    }

    /**
     * Whether the character is a opening bracket type.
     *
     * @param character Character to check.
     * @return Whether the input is a opening bracket.
     */
    private boolean isOpeningBracket(char character) {
        return character == PARENTHESIS_OPEN
                || character == SQUARE_BR_OPEN
                || character == CURLY_BR_OPEN
                || character == ANGLE_BR_OPEN;
    }

    /**
     * Whether the inputs resemble a pair of brackets.
     *
     * @param opening Opening bracket.
     * @param closing Closing bracket.
     * @return Whether the opening/closing brackets are matching brackets.
     */
    private boolean isBracketPair(char opening, char closing) {
        return (opening == PARENTHESIS_OPEN && closing == PARENTHESIS_CLOSE)
                || (opening == SQUARE_BR_OPEN && closing == SQUARE_BR_CLOSE)
                || (opening == CURLY_BR_OPEN && closing == CURLY_BR_CLOSE)
                || (opening == ANGLE_BR_OPEN && closing == ANGLE_BR_CLOSE);
    }
}
