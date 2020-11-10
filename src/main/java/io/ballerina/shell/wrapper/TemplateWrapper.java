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
package io.ballerina.shell.wrapper;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.Collection;

/**
 * Wrapper to wrap snippets with a code template.
 */
public class TemplateWrapper implements Wrapper {
    @Override
    public SyntaxTree wrap(Collection<Snippet<?>> imports,
                           Collection<Snippet<?>> moduleDeclarations,
                           Collection<Snippet<?>> statements, Snippet<?> expression) {
        String sourceCodeTemplate = readTemplate();
        String sourceCode = String.format(sourceCodeTemplate,
                toCodeLines(imports),
                toCodeLines(moduleDeclarations),
                toCodeLines(statements),
                expression.toSourceCode());
        TextDocument textDocument = TextDocuments.from(sourceCode);
        return SyntaxTree.from(textDocument);
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
        return "import ballerina/io;\n" +
                "%s\n" +
                "%s\n" +
                "function stmts() returns error? {\n" +
                "    %s\n" +
                "}\n" +
                "function do_it() returns string|error{\n" +
                "    check stmts();\n" +
                "    any|error expr = %s;\n" +
                "    any value = checkpanic expr;\n" +
                "    string repr = io:sprintf(\"%%s\", value);\n" +
                "    return repr;\n" +
                "}\n" +
                "public function main(){\n" +
                "    string|error result = trap do_it();\n" +
                "    if (result is string) {\n" +
                "        io:println(result);\n" +
                "    } else {\n" +
                "         io:println(\"Error occurred: \", result.message());\n" +
                "    }\n" +
                "}";
    }
}
