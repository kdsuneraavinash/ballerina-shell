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

package io.ballerina.shell.executor.desugar;

import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

/**
 * Simple data structure to hold binding information.
 */
public class Binding {
    private final TypeDescriptorNode type;
    private final BindingPatternNode bind;

    public Binding(TypeDescriptorNode type, BindingPatternNode bind) {
        this.type = type;
        this.bind = bind;
    }

    public static Binding fromNode(Node node) {
        TypedBindingPatternNode bindingPatternNode;
        if (node instanceof ModuleVariableDeclarationNode) {
            bindingPatternNode = ((ModuleVariableDeclarationNode) node).typedBindingPattern();
        } else if (node instanceof VariableDeclarationNode) {
            bindingPatternNode = ((VariableDeclarationNode) node).typedBindingPattern();
        } else {
            throw new RuntimeException("Can only bind a variable declaration.");
        }
        return new Binding(bindingPatternNode.typeDescriptor(), bindingPatternNode.bindingPattern());
    }

    public TypeDescriptorNode getType() {
        return type;
    }

    public BindingPatternNode getBind() {
        return bind;
    }

    /**
     * Identifier name of a binding is the variable name/id associated with it.
     * This can only be retrieved from bindings which are captured bindings.
     *
     * @return Name of the identifier
     */
    public String getIdentifierName() {
        assert getBind() instanceof CaptureBindingPatternNode;
        return ((CaptureBindingPatternNode) getBind()).variableName().text();
    }

    @Override
    public String toString() {
        return String.format("Binding[%s, %s]", type.toSourceCode().trim(), bind.toSourceCode().trim());
    }
}
