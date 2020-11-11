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
package io.ballerina.shell.executor.wrapper;

import io.ballerina.shell.snippet.Snippet;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Scanner;

/**
 * Wrapper to wrap snippets with a code template.
 */
public class TemplateWrapper implements Wrapper {
    private static final String SPECIAL_DELIMITER = "\\A";
    private static final String TEMPLATE_FILE = "template.bal";

    @Override
    public String wrap(Collection<Snippet<?>> imports,
                       Collection<Snippet<?>> moduleDeclarations,
                       Collection<Snippet<?>> variableDeclarationSnippets,
                       Collection<Snippet<?>> statements, Snippet<?> expression) {
        String sourceCodeTemplate = readTemplate();
        return String.format(sourceCodeTemplate,
                toCodeLines(imports),
                toCodeLines(moduleDeclarations),
                toCodeLines(variableDeclarationSnippets),
                toCodeLines(statements),
                expression.toSourceCode());
    }

    /**
     * Helper method to convert snippets into lines of code.
     * Each source code of snippets will be joined by new line.
     *
     * @param snippets Snippets.
     * @return Joined source code.
     */
    private String toCodeLines(Collection<Snippet<?>> snippets) {
        StringBuilder generated = new StringBuilder();
        for (Snippet<?> snippet : snippets) {
            generated.append(snippet.toSourceCode()).append('\n');
        }
        return generated.toString();
    }

    /**
     * Read the template for the code gen.
     *
     * @return Read the template from a resource file.
     */
    private String readTemplate() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(TEMPLATE_FILE);
        Objects.requireNonNull(inputStream, "File open failed");
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(reader).useDelimiter(SPECIAL_DELIMITER);
        return scanner.hasNext() ? scanner.next() : "";
    }
}
