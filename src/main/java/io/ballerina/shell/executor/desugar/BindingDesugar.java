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

import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ErrorBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ListBindingPatternNode;
import io.ballerina.compiler.syntax.tree.MappingBindingPatternNode;
import io.ballerina.compiler.syntax.tree.WildcardBindingPatternNode;
import io.ballerina.shell.snippet.VariableDefinitionSnippet;
import io.ballerina.shell.utils.diagnostics.ShellDiagnosticProvider;

import java.util.List;

/**
 * Converts variable definitions into simple bindings.
 * The initial definitions could be simple, list, array, map,... bindings.
 */
public class BindingDesugar implements Desugar<VariableDefinitionSnippet, Binding> {
    @Override
    public List<Binding> desugar(VariableDefinitionSnippet snippet) {
        return desugar(Binding.fromNode(snippet.getVariableNode()));
    }

    /**
     * Desugar the binding patterns found and get the variable binding simple form.
     * The output will be a list of bindings that were found in the base bindings.
     * The output binding will <b>always</b> be of {@code CaptureBindingPattern}.
     * <p/>
     * TODO: List binding patterns and mapping binding patterns are not supported in module level.
     * <pre>
     * {@code
     * binding-pattern :=
     *    capture-binding-pattern
     *    | wildcard-binding-pattern
     *    | list-binding-pattern
     *    | mapping-binding-pattern
     *    | error-binding-pattern}
     *
     * Type descriptors:
     *   capture-binding-pattern    - the type descriptor for that variable
     *   wildcard-binding-pattern   - any
     *   list-binding-pattern       - a tuple type descriptor
     *   mapping-binding-pattern    - a record type descriptor
     *   error-binding-pattern      - error type descriptor
     * </pre>
     *
     * @param binding The base binding pattern.
     * @return Desugared (Capture Binding) patterns.
     */
    public List<Binding> desugar(Binding binding) {
        if (binding.getBind() instanceof CaptureBindingPatternNode) {
            // int i = 4;
            return List.of(binding);
        } else if (binding.getBind() instanceof WildcardBindingPatternNode) {
            // int _ = 3;
            return List.of();
        } else if (binding.getBind() instanceof ListBindingPatternNode) {
            // [int, int] [a, b] = [2, 4];
            // These are rejected with a unhandled error when defined in module level
            ShellDiagnosticProvider.sendMessage("List binding pattern detected. Rejecting.");
            throw new RuntimeException("List bindings cannot be defined in the REPL.");
        } else if (binding.getBind() instanceof MappingBindingPatternNode) {
            // Person {name:myName, age:myAge} = getPerson();
            // These are rejected with a unhandled error when defined in module level
            ShellDiagnosticProvider.sendMessage("Mapping binding pattern detected. Rejecting.");
            throw new RuntimeException("Mapping bindings cannot be defined in the REPL.");
        } else if (binding.getBind() instanceof ErrorBindingPatternNode) {
            // TODO: Implement this pattern
            throw new RuntimeException("Error bindings are not supported yet.");
        }
        throw new RuntimeException("Sorry this binding type is not supported: " + binding.toString());
    }
}
