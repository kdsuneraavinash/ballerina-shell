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

import freemarker.template.Template;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    protected static final String MODULE_INIT_CLASS_NAME = "$_init";
    // TODO: Add a better way to set these
    protected static final String BALLERINA_HOME = "ballerina.home";
    private static final String TEMPLATE_FILE = "template.replay.ftl";

    protected final List<Snippet> imports;
    protected final List<Snippet> varDclns;
    protected final List<Snippet> moduleDclns;
    // TODO: Find a better alternative than a pair
    // The second value of the pair signifies whether the statement is a
    // statement snippet. (It could also be a expression)
    protected final List<Pair<Snippet, Boolean>> stmts;
    protected final Path ballerinaRuntime;
    protected final String templateName;
    protected Template template;

    public ReplayInvoker(Path ballerinaRuntime, Path ballerinaHome) {
        System.setProperty(BALLERINA_HOME, ballerinaHome.toString());
        this.imports = new ArrayList<>();
        this.varDclns = new ArrayList<>();
        this.moduleDclns = new ArrayList<>();
        this.stmts = new ArrayList<>();
        this.templateName = TEMPLATE_FILE;
        this.ballerinaRuntime = ballerinaRuntime;
    }

    @Override
    public void initialize() throws InvokerException {
        this.template = getTemplate(templateName);
        ReplayContext emptyContext = new ReplayContext(List.of(), List.of(), List.of(), List.of(), null);
        File mainBal = writeToFile(template, emptyContext);
        SingleFileProject project = SingleFileProject.load(mainBal.toPath());
        execute(project, JBallerinaBackend.from(compile(project), JdkVersion.JAVA_11));
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
        File mainBal = writeToFile(template, context);

        SingleFileProject project = SingleFileProject.load(mainBal.toPath());
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);

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
        return Objects.requireNonNullElse(this.template, super.getTemplate(templateName));
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
            int exitCode = runCommand(commands);
            return exitCode == 0;
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("Starting the executable failed: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (InterruptedException e) {
            addDiagnostic(Diagnostic.error("Exception while waiting for process to finish: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }

    /**
     * Runs a command given. Returns the exit code from the execution.
     *
     * @param commands Command to run.
     * @return Exit code of the command.
     * @throws IOException          If process starting failed.
     * @throws InterruptedException If interrupted.
     */
    protected int runCommand(List<String> commands) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commands).inheritIO();
        Process process = processBuilder.start();
        process.waitFor();
        return process.exitValue();
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
        cp.add(ballerinaRuntime.toAbsolutePath().toString());
        return cp.toString();
    }

    @Override
    public String toString() {
        return String.format("Replay Invoker State[imports = %s,  varDclns = %s,  moduleDclns = %s,  stmts = %s]",
                imports.size(), varDclns.size(), moduleDclns.size(), stmts.size());
    }
}
