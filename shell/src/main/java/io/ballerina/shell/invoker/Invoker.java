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

package io.ballerina.shell.invoker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JdkVersion;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.DiagnosticReporter;
import io.ballerina.shell.exceptions.InvokerException;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.utils.Pair;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Invoker that invokes a command to evaluate a list of snippets.
 * <p>
 * State of an invoker persists all the information required.
 * {@code reset} function will clear the invoker state.
 * <p>
 * Context of an invoker is the context that will be used to
 * fill the template. This should be a logic-less as much as possible.
 * Invoker and its context may be tightly coupled.
 */
public abstract class Invoker extends DiagnosticReporter {
    /**
     * Initializes the invoker. This can be used to load required files
     * and create caches. Calling this is not a requirement.
     * <p>
     * Runs so that a demo file is loaded and compiled
     * so the required caches will be ready once the user gives input.
     * Any error is an indication of a failure in template of base compilation.
     * Throw if that happens.
     *
     * @throws InvokerException If initialization failed.
     */
    public abstract void initialize() throws InvokerException;

    /**
     * Reset executor state so that the execution can be start over.
     */
    public abstract void reset();

    /**
     * Executes a snippet and returns the output lines.
     * Snippets parameter should only include newly added snippets.
     * Old snippets should be managed as necessary by the implementation.
     *
     * @param newSnippet New snippet to execute.
     * @return Execution output result.
     */
    public abstract Pair<Boolean, Optional<Object>> execute(Snippet newSnippet) throws InvokerException;

    /**
     * Returns available imports in the module.
     *
     * @return Available imports as a string.
     */
    public abstract String availableImports();

    /**
     * Returns available variables in the module.
     *
     * @return Available variables as a string.
     */
    public abstract String availableVariables();

    /**
     * Returns available declarations in the module.
     *
     * @return Available declarations as a string.
     */
    public abstract String availableModuleDeclarations();

    /**
     * Helper method that creates the template reference.
     *
     * @param templateName Name of the template.
     * @return Created template
     * @throws InvokerException If reading template failed.
     */
    protected Template getTemplate(String templateName) throws InvokerException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);
        cfg.setClassForTemplateLoading(getClass(), "/");
        cfg.setDefaultEncoding("UTF-8");
        try {
            Template template = cfg.getTemplate(templateName);
            String message = String.format("Using %s invoker on %s file.", getClass().getSimpleName(), templateName);
            addDiagnostic(Diagnostic.debug(message));
            return template;
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("Template file read failed: " + e.getMessage()));
            throw new InvokerException();
        }
    }

    /**
     * Helper method to compile a project and report any errors.
     *
     * @param project Project to compile.
     * @return Compilation data.
     * @throws InvokerException If compilation failed.
     */
    protected PackageCompilation compile(Project project) throws InvokerException {
        try {
            PackageCompilation packageCompilation = project.currentPackage().getCompilation();
            JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(packageCompilation, JdkVersion.JAVA_11);
            DiagnosticResult diagnosticResult = jBallerinaBackend.diagnosticResult();
            Module module = project.currentPackage().getDefaultModule();
            for (io.ballerina.tools.diagnostics.Diagnostic diagnostic : diagnosticResult.diagnostics()) {
                DiagnosticSeverity severity = diagnostic.diagnosticInfo().severity();
                if (severity == DiagnosticSeverity.ERROR) {
                    addDiagnostic(Diagnostic.error(highlightedDiagnostic(module, diagnostic)));
                } else if (severity == DiagnosticSeverity.WARNING) {
                    addDiagnostic(Diagnostic.warn(highlightedDiagnostic(module, diagnostic)));
                } else {
                    addDiagnostic(Diagnostic.debug(diagnostic.message()));
                }
            }
            if (diagnosticResult.hasErrors()) {
                addDiagnostic(Diagnostic.error("Compilation aborted because of errors."));
                throw new InvokerException();
            }
            return packageCompilation;
        } catch (InvokerException e) {
            throw e;
        } catch (Exception e) {
            addDiagnostic(Diagnostic.error("Something went wrong: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (Error e) {
            addDiagnostic(Diagnostic.error("Something severely went wrong: " + e));
            throw new InvokerException(e);
        }
    }

    /**
     * Highlight and show the error position.
     *
     * @param module     Module object. This should be a single document module.
     * @param diagnostic Diagnostic to show.
     * @return The string with position highlighted.
     */
    private String highlightedDiagnostic(Module module, io.ballerina.tools.diagnostics.Diagnostic diagnostic) {
        // Get the source code
        Optional<DocumentId> documentId = module.documentIds().stream().findFirst();
        assert documentId.isPresent();
        Document document = module.document(documentId.get());
        return Diagnostic.highlightDiagnostic(document.textDocument(), diagnostic);
    }

    /**
     * Helper method to write a template populated with context to a file.
     * Writes to a temporary file and returns the file obj.
     *
     * @param template Template name.
     * @param context  Context object to use to populate.
     * @return The created temp file.
     * @throws InvokerException If writing was unsuccessful.
     */
    protected File writeToFile(Template template, Object context) throws InvokerException {
        try {
            File createdFile = File.createTempFile("main-", ".bal");
            try (FileWriter fileWriter = new FileWriter(createdFile, Charset.defaultCharset())) {
                template.process(context, fileWriter);
            }
            createdFile.deleteOnExit();
            return createdFile;
        } catch (TemplateException e) {
            addDiagnostic(Diagnostic.error("Template processing failed: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("File generation failed: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }
}
