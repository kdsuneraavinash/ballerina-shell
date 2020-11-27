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

/**
 *
 */
public class GlobalLoadModifier extends TreeModifier {
    private final Map<String, String> allVars;

    public GlobalLoadModifier(Map<String, String> allVars) {
        this.allVars = allVars;
    }

    public FunctionBodyBlockNode transform(FunctionBodyBlockNode functionBodyBlockNode) {
        List<StatementNode> statementNodes = new ArrayList<>(loadStatements());
        functionBodyBlockNode.statements().forEach(statementNodes::add);
        return functionBodyBlockNode.modify().withStatements(NodeFactory.createNodeList(statementNodes)).apply();
    }

    private Collection<StatementNode> loadStatements() {
        List<StatementNode> statementNodes = new ArrayList<>();
        for (String varName : allVars.keySet()) {
            statementNodes.add(loadStatement(varName, allVars.get(varName)));
        }
        return statementNodes;
    }

    public StatementNode loadStatement(String varName, String typeName) {
        String sourceCode = String.format("function main(){%s = <%s> recall_var(\"%s\");}", varName, typeName, varName);
        return extractExpression(sourceCode);
    }

    public StatementNode extractExpression(String sourceCode) {
        TextDocument document = TextDocuments.from(sourceCode);
        SyntaxTree tree = SyntaxTree.from(document);
        ModulePartNode node = tree.rootNode();
        NodeList<ModuleMemberDeclarationNode> moduleDclns = node.members();
        ModuleMemberDeclarationNode moduleDeclaration = moduleDclns.get(0);
        FunctionDefinitionNode mainFunction = (FunctionDefinitionNode) moduleDeclaration;
        FunctionBodyBlockNode mainFunctionBody = (FunctionBodyBlockNode) mainFunction.functionBody();
        return mainFunctionBody.statements().get(0);
    }
}
