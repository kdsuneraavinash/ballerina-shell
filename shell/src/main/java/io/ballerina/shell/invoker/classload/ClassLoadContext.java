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

import freemarker.ext.beans.TemplateAccessible;
import io.ballerina.shell.utils.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Context that is used to populate the template
 * of {@link ClassLoadInvoker} objects.
 */
public class ClassLoadContext {
    private static final Pair<String, Boolean> DEFAULT_RETURN_EXPR = new Pair<>("()", false);
    private final String contextId;
    private final Collection<String> imports;
    private final Collection<String> moduleDclns;
    private final String lastVarDcln;
    private final Collection<Variable> varDclns;
    private final Pair<String, Boolean> lastExpr;

    /**
     * Creates a context for class load invoker.
     * A simple data class which is bound to the template.
     * Of {@code varDclns} and {@code saveVarDclns}, the first value
     * should be the type descriptor. Second value should be the variable name.
     *
     * @param contextId   Id of the context to use in memory.
     * @param imports     Import declarations.
     * @param moduleDclns Module level declaration.
     * @param lastVarDcln Last variable declaration if the last snippet was a var dcln.
     *                    If not, this should be null.
     * @param varDclns    Variable declarations to initialize with values.
     * @param lastExpr    Last expression if last value was a statement or an expression.
     */
    public ClassLoadContext(String contextId,
                            Collection<String> imports,
                            Collection<String> moduleDclns,
                            Collection<Variable> varDclns,
                            String lastVarDcln,
                            Pair<String, Boolean> lastExpr) {
        this.lastExpr = Objects.requireNonNullElse(lastExpr, DEFAULT_RETURN_EXPR);
        this.lastVarDcln = Objects.requireNonNullElse(lastVarDcln, "");
        this.contextId = Objects.requireNonNull(contextId);
        this.imports = Objects.requireNonNull(imports);
        this.varDclns = Objects.requireNonNull(varDclns);
        this.moduleDclns = Objects.requireNonNull(moduleDclns);
    }

    /**
     * Creates a context for class load invoker without new expression.
     *
     * @param contextId   Id of the context to use in memory.
     * @param imports     Import declarations.
     * @param moduleDclns Module level declaration.
     * @param lastVarDcln Last variable declaration if the last snippet was a var dcln.
     *                    If not, this should be null.
     * @param varDclns    Variable declarations to initialize with values.
     */
    public ClassLoadContext(String contextId,
                            Collection<String> imports,
                            Collection<String> moduleDclns,
                            Collection<Variable> varDclns,
                            String lastVarDcln) {
        this(contextId, imports, moduleDclns, varDclns, lastVarDcln, null);
    }

    /**
     * Creates a context for class load invoker with only imports.
     *
     * @param contextId Id of the context to use in memory.
     * @param imports   Import declarations.
     */
    public ClassLoadContext(String contextId, Collection<String> imports) {
        this(contextId, imports, List.of(), List.of(), null);
    }

    /**
     * Creates an empty context for class load invoker.
     *
     * @param contextId Id of the context to use in memory.
     */
    public ClassLoadContext(String contextId) {
        this(contextId, List.of(), List.of(), List.of(), null);
    }

    @TemplateAccessible
    public Collection<String> getImports() {
        return imports;
    }

    @TemplateAccessible
    public Collection<String> getModuleDclns() {
        return moduleDclns;
    }

    @TemplateAccessible
    public String getLastVarDcln() {
        return lastVarDcln;
    }

    @TemplateAccessible
    public Pair<String, Boolean> getLastExpr() {
        return lastExpr;
    }

    @TemplateAccessible
    public Collection<Variable> getVarDclns() {
        return varDclns;
    }

    @TemplateAccessible
    public String getContextId() {
        return contextId;
    }

    @TemplateAccessible
    public String getExprVarName() {
        return ClassLoadInvoker.EXPR_VAR_NAME;
    }

    @TemplateAccessible
    public String getCursorName() {
        return ClassLoadInvoker.CURSOR_NAME;
    }

    @TemplateAccessible
    public String getMemoryRef() {
        return ClassLoadMemory.class.getCanonicalName();
    }

    /**
     * A class to denote a variable declaration.
     * The {@code isNew} defines whether the variable was newly added.
     * For old variables, there should be an entry in the static memory class.
     */
    public static class Variable {
        private final String name;
        private final String type;
        private final boolean isNew;

        private Variable(String name, String type, boolean isNew) {
            this.name = name;
            this.type = type;
            this.isNew = isNew;
        }

        public static Variable newVar(String name, String type) {
            return new Variable(name, type, true);
        }

        public static Variable oldVar(String name, String type) {
            return new Variable(name, type, false);
        }

        @TemplateAccessible
        public String getName() {
            return name;
        }

        @TemplateAccessible
        public String getType() {
            return type;
        }

        @TemplateAccessible
        public boolean isNew() {
            return isNew;
        }
    }
}
