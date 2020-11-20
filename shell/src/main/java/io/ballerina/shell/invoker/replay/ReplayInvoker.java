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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JarResolver;
import io.ballerina.projects.JdkVersion;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.exceptions.InvokerException;
import io.ballerina.shell.invoker.Invoker;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.utils.Pair;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Executes the snippet given.
 * Re evaluates the snippet by generating a file containing all snippets
 * and executing it. The Project API will be used to compile the file.
 */
public class ReplayInvoker extends Invoker {
    private static final String MODULE_INIT_CLASS_NAME = "$_init";
    // TODO: Add a better way to set these
    private static final String BALLERINA_HOME = "ballerina.home";
    private static final Path BALLERINA_RUNTIME = Paths.get("home/bre/lib/*");
    private static final Path BALLERINA_HOME_PATH = Paths.get("home");

    private final List<Snippet> imports;
    private final List<Snippet> varDclns;
    private final List<Snippet> moduleDclns;
    // TODO: Find a better alternative than a pair
    // The second value of the pair signifies whether the statement is a
    // statement snippet. (It could also be a expression)
    private final List<Pair<Snippet, Boolean>> stmts;
    private final String generatedBallerinaFile;
    private final String templateName;
    private Template template;

    public ReplayInvoker(String templateName, String generatedBallerinaFile) {
        System.setProperty(BALLERINA_HOME, BALLERINA_HOME_PATH.toString());
        this.imports = new ArrayList<>();
        this.varDclns = new ArrayList<>();
        this.moduleDclns = new ArrayList<>();
        this.stmts = new ArrayList<>();
        this.templateName = templateName;
        this.generatedBallerinaFile = generatedBallerinaFile;
    }

    @Override
    public void reset() {
        imports.clear();
        varDclns.clear();
        moduleDclns.clear();
        stmts.clear();
    }

    @Override
    public boolean execute(Snippet newSnippet) throws InvokerException {
        this.template = getTemplate(templateName);
        ReplayContext context = createContext(newSnippet);
        try (FileWriter fileWriter = new FileWriter(generatedBallerinaFile, Charset.defaultCharset())) {
            template.process(context, fileWriter);
        } catch (TemplateException e) {
            addDiagnostic(Diagnostic.error("Template processing failed: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("File generation failed: " + e.getMessage()));
            throw new InvokerException(e);
        }

        SingleFileProject project = SingleFileProject.load(Paths.get(generatedBallerinaFile));
        JBallerinaBackend jBallerinaBackend = compile(project);

        boolean isSuccess = true;
        if (newSnippet.isExecutable()) {
            isSuccess = execute(project, jBallerinaBackend);
        }

        if (isSuccess) {
            addDiagnostic(Diagnostic.debug("Adding the snippet to memory."));
            if (newSnippet.isImport()) {
                imports.add(newSnippet);
            } else if (newSnippet.isVariableDeclaration()) {
                varDclns.add(newSnippet);
            } else if (newSnippet.isModuleMemberDeclaration()) {
                moduleDclns.add(newSnippet);
            } else if (newSnippet.isStatement()) {
                stmts.add(new Pair<>(newSnippet, true));
            } else if (newSnippet.isExpression()) {
                stmts.add(new Pair<>(newSnippet, false));
            }
        }
        return isSuccess;
    }

    /**
     * Creates the context object to be passed to template.
     * The new snippets are not added here. Instead they are added to copies.
     *
     * @param newSnippet New snippet from user.
     * @return Created context.
     */
    protected ReplayContext createContext(Snippet newSnippet) {
        List<String> importStrings = new ArrayList<>();
        List<String> varDclnStrings = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>();
        List<Pair<String, Boolean>> stmtStrings = new ArrayList<>();

        imports.stream().map(Objects::toString).forEach(importStrings::add);
        varDclns.stream().map(Objects::toString).forEach(varDclnStrings::add);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        stmts.stream().map(p -> new Pair<>(p.getFirst().toString(), p.getSecond())).forEach(stmtStrings::add);

        Pair<String, Boolean> lastExpr = null;
        if (newSnippet.isImport()) {
            importStrings.add(newSnippet.toString());
        } else if (newSnippet.isVariableDeclaration()) {
            varDclnStrings.add(newSnippet.toString());
        } else if (newSnippet.isModuleMemberDeclaration()) {
            moduleDclnStrings.add(newSnippet.toString());
        } else if (newSnippet.isStatement()) {
            lastExpr = new Pair<>(newSnippet.toString(), true);
        } else if (newSnippet.isExpression()) {
            lastExpr = new Pair<>(newSnippet.toString(), false);
        }

        return new ReplayContext(importStrings, varDclnStrings, moduleDclnStrings, stmtStrings, lastExpr);
    }

    /**
     * Creates the template reference.
     * If the template is already created, will return the created one instead.
     *
     * @param templateName Name of the template.
     * @return Created template
     * @throws InvokerException If reading template failed.
     */
    protected Template getTemplate(String templateName) throws InvokerException {
        if (this.template != null) {
            return this.template;
        }

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
     * Compile a project and report any errors.
     *
     * @param project Project to compile.
     * @return Created backend.
     * @throws InvokerException If compilation failed.
     */
    protected JBallerinaBackend compile(Project project) throws InvokerException {
        PackageCompilation packageCompilation = project.currentPackage().getCompilation();
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(packageCompilation, JdkVersion.JAVA_11);
        DiagnosticResult diagnosticResult = jBallerinaBackend.diagnosticResult();
        for (io.ballerina.tools.diagnostics.Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            DiagnosticSeverity severity = diagnostic.diagnosticInfo().severity();
            // TODO: Add smart error highlighting.
            if (severity == DiagnosticSeverity.ERROR) {
                addDiagnostic(Diagnostic.error(diagnostic.message()));
            } else if (severity == DiagnosticSeverity.WARNING) {
                addDiagnostic(Diagnostic.warn(diagnostic.message()));
            } else {
                addDiagnostic(Diagnostic.debug(diagnostic.message()));
            }
        }
        if (diagnosticResult.hasErrors()) {
            addDiagnostic(Diagnostic.error("Compilation aborted because of errors."));
            throw new InvokerException();
        }
        return jBallerinaBackend;
    }

    /**
     * Executes a compiled project.
     * It is expected that the project had no compiler errors.
     * Will run the process using the same IO and wait for it to finish.
     *
     * @param project           Project to run.
     * @param jBallerinaBackend Backed to use.
     * @return Whether process execution was successful.
     * @throws InvokerException If execution failed.
     */
    protected boolean execute(Project project, JBallerinaBackend jBallerinaBackend) throws InvokerException {
        if (!project.currentPackage().getDefaultModule().getCompilation().entryPointExists()) {
            addDiagnostic(Diagnostic.error("Unexpected Error: No entry point!!!"));
            throw new InvokerException();
        }

        try {
            Module executableModule = project.currentPackage().getDefaultModule();
            JarResolver jarResolver = jBallerinaBackend.jarResolver();
            String initClassName = JarResolver.getQualifiedClassName(
                    executableModule.packageInstance().packageOrg().toString(),
                    executableModule.packageInstance().packageName().toString(),
                    executableModule.packageInstance().packageVersion().toString(),
                    MODULE_INIT_CLASS_NAME);

            List<String> commands = List.of("java", "-cp", getAllClassPaths(jarResolver), initClassName);
            ProcessBuilder processBuilder = new ProcessBuilder(commands).inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("Starting the executable failed: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (InterruptedException e) {
            addDiagnostic(Diagnostic.error("Exception while waiting for process to finish: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }

    /**
     * Construct the joined class path required to execute the jar created by resolver.
     * Need to add the BRE location to the class path as well.
     *
     * @param jarResolver Jar resolver of the project.
     * @return Joined classpath.
     */
    private String getAllClassPaths(JarResolver jarResolver) {
        StringJoiner cp = new StringJoiner(File.pathSeparator);
        jarResolver.getJarFilePathsRequiredForExecution().stream().map(Path::toString).forEach(cp::add);
        cp.add(BALLERINA_RUNTIME.toAbsolutePath().toString());
        return cp.toString();
    }

    @Override
    public String toString() {
        return String.format("Replay Invoker State[imports = %s,  varDclns = %s,  moduleDclns = %s,  stmts = %s]",
                imports.size(), varDclns.size(), moduleDclns.size(), stmts.size());
    }
}
