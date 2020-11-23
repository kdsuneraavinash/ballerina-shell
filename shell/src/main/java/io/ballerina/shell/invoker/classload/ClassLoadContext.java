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
    private final Collection<String> imports;
    private final Collection<String> moduleDclns;
    private final String lastVarDcln;
    private final List<Pair<String, String>> initVarDclns;
    private final List<Pair<String, String>> saveVarDclns;
    private final Pair<String, Boolean> lastExpr;

    /**
     * Creates a context for class load invoker.
     * A simple data class which is bound to the template.
     * Of {@code initVarDclns} and {@code saveVarDclns}, the first value
     * should be the type descriptor. Second value should be the variable name.
     *
     * @param imports      Import declarations.
     * @param moduleDclns  Module level declaration.
     * @param lastVarDcln  Last variable declaration if the last snippet was a var dcln.
     *                     If not, this should be null.
     * @param initVarDclns Variable declarations to initialize with values.
     * @param saveVarDclns Variable declarations to save.
     * @param lastExpr     Last expression if last value was a statement or an expression.
     *                     The second value should be a boolean indicating whether the last one was an statement.
     */
    public ClassLoadContext(Collection<String> imports,
                            Collection<String> moduleDclns,
                            String lastVarDcln,
                            List<Pair<String, String>> initVarDclns,
                            List<Pair<String, String>> saveVarDclns,
                            Pair<String, Boolean> lastExpr) {
        this.lastExpr = Objects.requireNonNullElse(lastExpr, DEFAULT_RETURN_EXPR);
        this.imports = imports;
        this.lastVarDcln = Objects.requireNonNullElse(lastVarDcln, "");
        this.initVarDclns = initVarDclns;
        this.saveVarDclns = saveVarDclns;
        this.moduleDclns = moduleDclns;
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
    public Collection<Pair<String, String>> getInitVarDclns() {
        return initVarDclns;
    }

    @TemplateAccessible
    public List<Pair<String, String>> getSaveVarDclns() {
        return saveVarDclns;
    }
}
