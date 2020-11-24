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
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.Pair;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Executes the snippet given.
 * This invoker will save all the variable values in a static class and
 * load them into the generated class effectively managing any side-effects.
 * TODO: Test this with various sessions.
 */
public class ClassLoadInvoker extends Invoker {
    protected static final String MODULE_INIT_CLASS_NAME = "$_init";
    protected static final String MODULE_MAIN_METHOD_NAME = "main";
    private static final String TEMPLATE_FILE = "template.classload.ftl";
    // TODO: Set ballerina home using system prop.
    protected static final String BALLERINA_HOME = "ballerina.home";
    protected static final Set<String> INIT_VAR_NAMES = Set.of("context_id", "$annotation_data");

    protected final List<Snippet> imports;
    protected final List<Snippet> moduleDclns;
    // First is name, second is type
    protected final List<Pair<String, String>> varDclns;
    protected final Set<String> globalVarNames;
    protected final String generatedBallerinaFile;
    protected final String templateName;
    protected final String contextId;
    protected Template template;

    public ClassLoadInvoker(String tmpFileName, Path ballerinaHome) {
        System.setProperty(BALLERINA_HOME, ballerinaHome.toString());
        this.imports = new ArrayList<>();
        this.varDclns = new ArrayList<>();
        this.moduleDclns = new ArrayList<>();
        this.templateName = TEMPLATE_FILE;
        this.generatedBallerinaFile = tmpFileName;
        this.contextId = UUID.randomUUID().toString();
        this.globalVarNames = new HashSet<>(INIT_VAR_NAMES);

    }

    @Override
    public void initialize() throws InvokerException {
        this.template = getTemplate(templateName);
        ClassLoadContext emptyContext = new ClassLoadContext(contextId, List.of(), List.of(), null,
                List.of(), List.of(), null);
        writeToFile(generatedBallerinaFile, template, emptyContext);
        SingleFileProject project = SingleFileProject.load(Paths.get(generatedBallerinaFile));
        execute(project, JBallerinaBackend.from(compile(project), JdkVersion.JAVA_11));
    }

    @Override
    public void reset() {
        imports.clear();
        varDclns.clear();
        moduleDclns.clear();
        // We need to clear the memory as well.
        ClassLoadMemory.forgetAll(contextId);
    }

    @Override
    public boolean execute(Snippet newSnippet) throws InvokerException {
        this.template = getTemplate(templateName);

        List<Pair<String, String>> newVariables = new ArrayList<>();
        if (newSnippet.isVariableDeclaration()) {
            VariableDeclarationSnippet varDcln = (VariableDeclarationSnippet) newSnippet;
            ClassLoadContext varTypeInferContext = createVarTypeInferContext(varDcln);
            writeToFile(generatedBallerinaFile, template, varTypeInferContext);
            SingleFileProject project = SingleFileProject.load(Paths.get(generatedBallerinaFile));
            PackageCompilation compilation = compile(project);
            List<BLangSimpleVariable> globalVariables = compilation.defaultModuleBLangPackage().getGlobalVariables();
            for (BLangSimpleVariable variable : globalVariables) {
                if (!globalVarNames.contains(variable.name.value)) {
                    newVariables.add(new Pair<>(variable.name.value, variable.type.toString()));
                }
            }
        }

        ClassLoadContext context = createContext(newSnippet, newVariables);
        writeToFile(generatedBallerinaFile, template, context);

        SingleFileProject project = SingleFileProject.load(Paths.get(generatedBallerinaFile));
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);
        boolean isSuccess = execute(project, jBallerinaBackend);

        if (isSuccess) {
            addDiagnostic(Diagnostic.debug("Adding the snippet to memory."));
            if (newSnippet.isImport()) {
                imports.add(newSnippet);
            } else if (newSnippet.isVariableDeclaration()) {
                varDclns.addAll(newVariables);
                newVariables.stream().map(Pair::getFirst).forEach(globalVarNames::add);
            } else if (newSnippet.isModuleMemberDeclaration()) {
                moduleDclns.add(newSnippet);
            }
        }
        return isSuccess;
    }


    /**
     * Creates a context which can be used to identify new variables.
     *
     * @param newSnippet New snippet. Must be a var dcln.
     * @return Context with type information inferring code.
     */
    protected ClassLoadContext createVarTypeInferContext(VariableDeclarationSnippet newSnippet) {
        List<String> importStrings = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>();
        List<Pair<String, String>> initVarDclns = new ArrayList<>(varDclns);
        List<Pair<String, String>> saveVarDclns = new ArrayList<>(varDclns);
        imports.stream().map(Objects::toString).forEach(importStrings::add);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        String lastVarDcln = newSnippet.toString();
        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings, lastVarDcln,
                initVarDclns, saveVarDclns, null);
    }

    /**
     * Creates the context object to be passed to template.
     * The new snippets are not added here. Instead they are added to copies.
     *
     * @param newSnippet   New snippet from user.
     * @param newVariables Newly defined variables. Must be set if snippet is a var dcln.
     * @return Created context.
     */
    protected ClassLoadContext createContext(Snippet newSnippet, List<Pair<String, String>> newVariables) {
        List<String> importStrings = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>();
        imports.stream().map(Objects::toString).forEach(importStrings::add);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        List<Pair<String, String>> initVarDclns = new ArrayList<>(varDclns);
        List<Pair<String, String>> saveVarDclns = new ArrayList<>(varDclns);

        // Variable declarations are handled differently.
        // If current snippet is a var dcln, it is added to saveVarDclns but not to initVarDclns.
        // All other var dclns are added to both.
        // Last expr is the last snippet if it was either a stmt or an expression.

        Pair<String, Boolean> lastExpr = null;
        String lastVarDcln = null;
        if (newSnippet.isImport()) {
            importStrings.add(newSnippet.toString());
        } else if (newSnippet.isVariableDeclaration()) {
            lastVarDcln = newSnippet.toString();
            saveVarDclns.addAll(newVariables);
        } else if (newSnippet.isModuleMemberDeclaration()) {
            moduleDclnStrings.add(newSnippet.toString());
        } else if (newSnippet.isStatement()) {
            lastExpr = new Pair<>(newSnippet.toString(), true);
        } else if (newSnippet.isExpression()) {
            lastExpr = new Pair<>(newSnippet.toString(), false);
        }

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings, lastVarDcln,
                initVarDclns, saveVarDclns, lastExpr);
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
     * The process is run and the stdout is collected and printed.
     * TODO: Collect and print stdout as the program runs. (Not after exit)
     * Due to ballerina calling system.exit(), we need to disable these calls and
     * remove system error logs as well.
     * TODO: Fix these issues.
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
            ClassLoader classLoader = jarResolver.getClassLoaderWithRequiredJarFilesForExecution();
            Class<?> clazz = classLoader.loadClass(initClassName);

            Method method = clazz.getDeclaredMethod(MODULE_MAIN_METHOD_NAME, String[].class);
            String[] args = new String[0];

            PrintStream stdErr = System.err;
            PrintStream stdOut = System.out;
            ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
            try {
                System.setErr(new PrintStream(new ByteArrayOutputStream()));
                System.setOut(new PrintStream(stdOutBaOs));
                System.setSecurityManager(new NoExitVmSecManager(System.getSecurityManager()));
                method.invoke(null, new Object[]{args});
            } catch (InvocationTargetException ignored) {
            } finally {
                // Restore everything
                stdOut.print(new String(stdOutBaOs.toByteArray()));
                System.setSecurityManager(null);
                System.setErr(stdErr);
                System.setOut(stdOut);
            }

            return true;
        } catch (ClassNotFoundException e) {
            addDiagnostic(Diagnostic.error("Main class not found: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (NoSuchMethodException e) {
            addDiagnostic(Diagnostic.error("Main method not found: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (IllegalAccessException e) {
            addDiagnostic(Diagnostic.error("Access for the method failed: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }
}
