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

package io.ballerina.shell.snippet.types;

import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ErrorBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ListBindingPatternNode;
import io.ballerina.compiler.syntax.tree.MappingBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgBindingPatternNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.RestBindingPatternNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WildcardBindingPatternNode;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetSubKind;
import io.ballerina.shell.utils.debug.DebugProvider;

import java.util.Optional;

/**
 * These will be variable declarations.
 * Currently only module level variable declarations are accepted.
 * TODO: Move variable value declaration into a statement snippet.
 */
public class VariableDeclarationSnippet extends Snippet {
    private final String variableName;
    private final boolean isSerializable;

    public VariableDeclarationSnippet(String sourceCode, String variableName, boolean isSerializable) {
        super(sourceCode, SnippetSubKind.VARIABLE_DECLARATION);
        this.variableName = variableName;
        this.isSerializable = isSerializable;
    }

    /**
     * Create a var definition snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static VariableDeclarationSnippet tryFromNode(Node node) {
        // Only variables that are allowed are module level variables.
        // So, all the variable declarations are converted into module
        // level variable declarations. However, there might be other
        // variable declarations as well. If that is the case attempt to
        // parse it as a module level variable by removing/adding some
        // additional information.
        // Binding pattern is also required to be analyzed so that
        // we can synthesize if the type is serializable.
        // Also some declarations might not give a initializer.
        // These are not allowed in module level. Using type we can
        // infer a default value.
        // # Module level declarations available children:
        //      metadata, finalKeyword, typedBindingPattern,
        //      equalsToken, initializer, semicolonToken
        // # Variable declarations available children:
        //      annotations, finalKeyword, typedBindingPattern,
        //      equalsToken, initializer, semicolonToken
        // annotations are valid metadata. So, inferring is doable.

        ModuleVariableDeclarationNode dclnNode;
        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varDcln = (VariableDeclarationNode) node;
            dclnNode = NodeFactory.createModuleVariableDeclarationNode(
                    NodeFactory.createMetadataNode(null, varDcln.annotations()),
                    varDcln.finalKeyword().orElse(null), varDcln.typedBindingPattern(),
                    varDcln.equalsToken().orElse(null), varDcln.initializer().orElse(null),
                    varDcln.semicolonToken()
            );
        } else if (node instanceof ModuleVariableDeclarationNode) {
            dclnNode = (ModuleVariableDeclarationNode) node;
        } else {
            return null;
        }

        // Find the type and try to infer the type.
        // There can be several types of variable declarations.
        // 1. int i = 0     -- has initializer, can infer default
        // 2. int i         -- no initializer, can infer default
        // 3. error i = f() -- has initializer, cant infer default
        // 4. error i       -- no initializer, cant infer default
        // In these ones, (1) and (3) does not need inferring.
        // (2) can be inferred. (4) is rejected.
        // So we need to infer only if the type is a doable type and
        // no initializer present.

        VariableType type = VariableType.fromDescriptor(dclnNode.typedBindingPattern().typeDescriptor());
        String sourceCode;
        if (dclnNode.initializer().isPresent()) {
            // Initializer present, no need to infer.
            sourceCode = dclnNode.toSourceCode();
        } else {
            // If inferring failed as well, throw an error message.
            if (type.getDefaultValue().isEmpty()) {
                throw new SnippetException("" +
                        "Initializer is required for variable declarations of this type.\n" +
                        "REPL will infer most of the types' default values, but this type could not be inferred.");
            }
            DebugProvider.sendMessage("Inferred default value: " + type.getDefaultValue().get());

            // Inject default value.
            sourceCode = NodeFactory.createModuleVariableDeclarationNode(
                    dclnNode.metadata().orElse(null), dclnNode.finalKeyword().orElse(null),
                    dclnNode.typedBindingPattern(), NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN),
                    type.getDefaultValue().get(), dclnNode.semicolonToken()
            ).toSourceCode();
        }

        // Now try to identify the variable name.
        Optional<String> variableName = identifyName(dclnNode.typedBindingPattern().bindingPattern());
        DebugProvider.sendMessage("Identified variable name: " + variableName);

        boolean isSerializable = type.isSerializable() && variableName.isPresent();
        if (isSerializable) {
            DebugProvider.sendMessage("The variable is a candidate for serialization.");
        }
        return new VariableDeclarationSnippet(sourceCode, variableName.orElse(null), isSerializable);
    }

    /**
     * Identifies tha variable name(s) that are defined in the declaration.
     * No identical names are defined in a binding pattern.
     * So a set of names are returned.
     *
     * @param bind Binding pattern enclosing the name.
     * @return Names that are defined in the statement.
     */
    private static Optional<String> identifyName(BindingPatternNode bind) {
        // Possible binds are,
        //   capture-binding-pattern, wildcard-binding-pattern, list-binding-pattern,
        //   mapping-binding-pattern, error-binding-pattern
        // However, mapping-binding-pattern and list-binding-pattern cannot be declared globally.
        // These will be disabled in the REPL.
        // error-binding-pattern will bind values to identifiers inside the error type.
        // So, these are not globally done.
        // TODO: Verify this statement.
        // wildcard-binding-pattern will not define any new variable name.
        // capture-binding-pattern are the only valid pattern.
        // Additionally rest-binding-pattern/named-arg-binding-pattern are also candidates
        // for binding pattern. But these cannot exist without map/list binding patterns.
        // So they are unexpected.

        if (bind instanceof ErrorBindingPatternNode || bind instanceof WildcardBindingPatternNode) {
            return Optional.empty();
        } else if (bind instanceof CaptureBindingPatternNode) {
            String variableName = ((CaptureBindingPatternNode) bind).variableName().text();
            return Optional.of(variableName);
        } else if (bind instanceof MappingBindingPatternNode) {
            throw new SnippetException("" +
                    "Map/Record bindings are disabled in module level.\n" +
                    "Declarations of format Record {a:p, b:q} = {p:x,q:y} cannot be done in global level.");
        } else if (bind instanceof ListBindingPatternNode) {
            throw new SnippetException("" +
                    "List bindings are disabled in module level.\n" +
                    "Declarations of format [int,int] [a,b] = [1,0] cannot be done in global level.");
        } else if (bind instanceof NamedArgBindingPatternNode || bind instanceof RestBindingPatternNode) {
            throw new SnippetException("" +
                    "Unexpected binding pattern found.\n" +
                    "Please check your statement for syntax errors.");
        } else {
            throw new SnippetException("Unknown variable bind: " + bind.getClass().getSimpleName());
        }
    }

    public String getVariableName() {
        return variableName;
    }

    public boolean isSerializable() {
        return isSerializable;
    }
}
