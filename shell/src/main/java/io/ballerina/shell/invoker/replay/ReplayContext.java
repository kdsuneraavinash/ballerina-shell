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

package io.ballerina.shell.invoker.replay;

import freemarker.ext.beans.TemplateAccessible;
import io.ballerina.shell.utils.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ReplayContext {
    private static final Pair<String, Boolean> DEFAULT_RETURN_EXPR = new Pair<>("()", false);
    private final Collection<String> imports;
    private final Collection<String> varDclns;
    private final Collection<String> moduleDclns;
    private final List<Pair<String, Boolean>> stmts;
    private final Pair<String, Boolean> lastExpr;

    public ReplayContext(Collection<String> imports,
                         Collection<String> varDclns,
                         Collection<String> moduleDclns,
                         List<Pair<String, Boolean>> stmts,
                         Pair<String, Boolean> lastExpr) {
        this.lastExpr = Objects.requireNonNullElse(lastExpr, DEFAULT_RETURN_EXPR);
        this.imports = imports;
        this.varDclns = varDclns;
        this.moduleDclns = moduleDclns;
        this.stmts = stmts;
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
    public Collection<String> getVarDclns() {
        return varDclns;
    }

    @TemplateAccessible
    public Collection<Pair<String, Boolean>> getStmts() {
        return stmts;
    }

    @TemplateAccessible
    public Pair<String, Boolean> getLastExpr() {
        return lastExpr;
    }
}
