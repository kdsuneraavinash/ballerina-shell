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

package io.ballerina.shell.executor.reeval;

import freemarker.ext.beans.TemplateAccessible;
import io.ballerina.shell.executor.Context;
import io.ballerina.shell.postprocessor.Postprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;

import java.util.List;
import java.util.Set;

/**
 * Mustache context for {@link ReEvalExecutor}.
 * The methods in this context would be consumed by the template file.
 */
public class ReEvalContext implements Context {
    /**
     * Statement expression containing a statement/expression.
     * Expressions needs to be formatted differently than a statement.
     * However, they need to be in order of appearance.
     */
    public static class StatementExpression {
        private final boolean isExpr;
        private final boolean isStmt;
        private final String code;

        public StatementExpression(Snippet snippet) {
            this.isExpr = snippet.getKind() == SnippetKind.EXPRESSION_KIND;
            this.isStmt = snippet.getKind() == SnippetKind.STATEMENT_KIND;
            this.code = snippet.toSourceCode();
        }

        @TemplateAccessible
        public String getCode() {
            return code;
        }

        @TemplateAccessible
        public boolean isExpr() {
            return isExpr;
        }

        @TemplateAccessible
        public boolean isStmt() {
            return isStmt;
        }
    }

    private final List<String> imports;
    private final List<String> moduleDclns;
    private final List<String> varDclns;
    private final List<StatementExpression> stmts;
    private final StatementExpression lastStmt;
    private final Set<String> varNames;

    public ReEvalContext(List<String> imports,
                         List<String> moduleDclns,
                         List<String> varDclns,
                         List<StatementExpression> stmts,
                         StatementExpression lastStmt,
                         Set<String> varNames) {
        this.imports = imports;
        this.moduleDclns = moduleDclns;
        this.varDclns = varDclns;
        this.stmts = stmts;
        this.lastStmt = lastStmt;
        this.varNames = varNames;
    }

    @TemplateAccessible
    public List<String> getImports() {
        return imports;
    }

    @TemplateAccessible
    public List<String> getModuleDclns() {
        return moduleDclns;
    }

    @TemplateAccessible
    public List<String> getVarDclns() {
        return varDclns;
    }

    @TemplateAccessible
    public List<StatementExpression> getStmts() {
        return stmts;
    }

    @TemplateAccessible
    public StatementExpression getLastStmt() {
        return lastStmt;
    }

    @TemplateAccessible
    public Set<String> getVarNames() {
        return varNames;
    }

    @TemplateAccessible
    @SuppressWarnings("SameReturnValue")
    public String getIoActivationStart() {
        return Postprocessor.ACTIVATION_START;
    }

    @TemplateAccessible
    @SuppressWarnings("SameReturnValue")
    public String getIoActivationEnd() {
        return Postprocessor.ACTIVATION_END;
    }
}

