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

package io.ballerina.shell.invoker.classload;

import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.shell.exceptions.InvokerException;
import io.ballerina.shell.snippet.types.ImportDeclarationSnippet;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses a type to find exported types and add required implicit imports.
 */
public class TypeParser {
    public static final String QUOTE = "'";
    /**
     * The regular expression used to parse the exported type.
     * Parsing format: org name    /   module name                 : version   :   type name
     * Formatted Vers: [ identifier/ ] identifier (. identifier)*  : [ {0-9.}* : ] identifier
     * Reg exp groups: (eg: ballerina/module1.module2.module3:2.2:type)
     * [0] exported type match (ballerina/module1.module2.module3:2.2:type)
     * [1] org name with slash (ballerina/)
     * [2] all module names sep with dot (module1.module2.module3)
     * [3] first module name (module1)
     * [4] last module name (module)
     * [5] type (type)
     */
    protected static final Pattern EXPORTED_NAME =
            Pattern.compile("" +
                    "([a-zA-Z0-9_$']+/)?" + // org name
                    "(([a-zA-Z0-9_']*)(?:\\.([a-zA-Z0-9_']*))*):" + // module name
                    "(?:[0-9.]*:)?" + // version
                    "([a-zA-Z0-9_']+)" // type name
            );

    private final ImportProcessor importProcessor;

    public TypeParser(ImportProcessor importProcessor) {
        this.importProcessor = importProcessor;
    }

    /**
     * Formats the type signature so that it can be used as a typedef.
     * Required implicit imports are added in this. Also empty <> are fixed.
     * TODO: Fix invisible type bug.
     *
     * @param signature              Unformatted type signature.
     * @param implicitImportPrefixes Set to add found imports.
     * @return Formatted type.
     */
    public String process(String signature, Set<String> implicitImportPrefixes) throws InvokerException {
        String type = processExportedTypes(signature, implicitImportPrefixes);
        return processTempSignatureIssues(type);
    }

    /**
     * Formats the type signature so that it can be used as a typedef. For example, int will be formatted to int.
     * ballerina/abc:1.0:pqr will be converted to 'imp1:pqr and an import added as import ballerina/abc.pqr as 'imp1.
     *
     * @param signature              Unformatted type signature.
     * @param implicitImportPrefixes Set to add found imports.
     * @return Formatted type.
     */
    public String processExportedTypes(String signature, Set<String> implicitImportPrefixes) throws InvokerException {
        Matcher matcher = EXPORTED_NAME.matcher(signature);
        AtomicBoolean isError = new AtomicBoolean(false);
        String formattedType = matcher.replaceAll((result -> {
            try {
                // Find required information to create import
                String orgName = quotedIdentifier(result.group(1));
                String moduleName = Arrays.stream(result.group(2)
                        .split("\\.")).map(this::quotedIdentifier)
                        .collect(Collectors.joining("."));
                String typeName = result.group(5);

                // If org name is anonymous, this is a declared type.
                if (orgName.equals("'$anon/")) {
                    return typeName;
                }

                // Create import snippet and find prefix using processor
                String importStatement = String.format("import %s%s;", orgName, moduleName);
                TextDocument textDocument = TextDocuments.from(importStatement);
                ModulePartNode modulePartNode = SyntaxTree.from(textDocument).rootNode();
                ImportDeclarationNode importDeclarationNode = modulePartNode.imports().get(0);
                ImportDeclarationSnippet snippet = new ImportDeclarationSnippet(importDeclarationNode);
                // TODO: Handle edge cases of import prefix already used.
                String importPrefix = importProcessor.processImport(snippet);
                implicitImportPrefixes.add(importPrefix);
                return String.format("%s:%s", importPrefix, typeName);
            } catch (InvokerException e) {
                isError.set(true);
                return result.group(0);
            }
        }));

        if (isError.get()) {
            throw new InvokerException();
        }
        return formattedType;
    }

    /**
     * Replaces several issues found in signature via temp hacks.
     *
     * @param type Type to format.
     * @return Formatted type.
     */
    private String processTempSignatureIssues(String type) {
        // TODO: Fix these issues in a better way.
        // map<never> gets converted to map<> (?)
        type = type.replace("<>", "<never>");
        // record {} gets converted to record {anydata...}
        type = type.replace("anydata...;", "");
        return type;
    }

    /**
     * Creates an quoted identifier to use for variable names.
     *
     * @param rawIdentifier Identifier without quote.
     * @return Quoted identifier.
     */
    private String quotedIdentifier(String rawIdentifier) {
        if (String.valueOf(rawIdentifier).startsWith(QUOTE)) {
            return rawIdentifier;
        }
        return QUOTE + rawIdentifier;
    }

    /**
     * Signature of a function that processes an import and returns its prefix.
     */
    public interface ImportProcessor {
        String processImport(ImportDeclarationSnippet importSnippet) throws InvokerException;
    }
}
