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

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TreeModifier;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A modifier that will inject global load variable value loading statements
 * to the start of a function block. This will result in all the globals used inside the
 * function being fully up-to-date.
 */
public class GlobalLoadModifier extends TreeModifier {
    private final Map<String, String> allVars;

    public GlobalLoadModifier(Map<String, String> allVars) {
        this.allVars = allVars;
    }

    @Override
    public FunctionBodyBlockNode transform(FunctionBodyBlockNode functionBodyBlockNode) {
        List<StatementNode> statementNodes = new ArrayList<>(loadStatements());
        functionBodyBlockNode.statements().forEach(statementNodes::add);
        return functionBodyBlockNode.modify().withStatements(NodeFactory.createNodeList(statementNodes)).apply();
    }

    /**
     * Creates the statements required to load the global var values.
     *
     * @return Statements required to restore global values.
     */
    private Collection<StatementNode> loadStatements() {
        String varStatement = allVars.entrySet().stream()
                .map(e -> String.format("%s = <%s> recall_var(\"%s\");", e.getKey(), e.getValue(), e.getKey()))
                .collect(Collectors.joining());
        String sourceCode = String.format("function main(){%s%n}", varStatement);
        List<StatementNode> statements = new ArrayList<>();
        extractStatements(sourceCode).forEach(statements::add);
        return statements;
    }

    /**
     * Extracts the statement from a function body.
     * Used to create the statements using a string.
     *
     * @param sourceCode Source code to use for extraction.
     * @return Statements inside the function.
     */
    private NodeList<StatementNode> extractStatements(String sourceCode) {
        TextDocument document = TextDocuments.from(sourceCode);
        SyntaxTree tree = SyntaxTree.from(document);
        ModulePartNode node = tree.rootNode();
        NodeList<ModuleMemberDeclarationNode> moduleDclns = node.members();
        ModuleMemberDeclarationNode moduleDeclaration = moduleDclns.get(0);
        FunctionDefinitionNode mainFunction = (FunctionDefinitionNode) moduleDeclaration;
        FunctionBodyBlockNode mainFunctionBody = (FunctionBodyBlockNode) mainFunction.functionBody();
        return mainFunctionBody.statements();
    }
}
