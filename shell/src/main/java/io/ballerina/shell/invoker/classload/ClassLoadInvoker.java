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
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String TEMPLATE_FILE = "template.classload.ftl";
    protected static final String BALLERINA_HOME = "ballerina.home";
    // Main class and method names to invoke
    protected static final String MODULE_INIT_CLASS_NAME = "$_init";
    protected static final String MODULE_MAIN_METHOD_NAME = "main";
    // Variables that are set from the start. These should not be cached.
    protected static final Set<String> INIT_VAR_NAMES = Set.of("context_id", "$annotation_data");

    protected final List<Snippet> imports;
    protected final List<Snippet> moduleDclns;
    protected final Map<String, String> globalVars;
    protected final String contextId;
    protected Template template;

    /**
     * Creates a class load invoker from the given ballerina home.
     * Ballerina home should be tha path that contains repo directory.
     * It is expected that the runtime is added in the class path.
     *
     * @param ballerinaHome Ballerina home directory.
     */
    public ClassLoadInvoker(Path ballerinaHome) {
        // TODO: Set ballerina home using system prop.
        System.setProperty(BALLERINA_HOME, ballerinaHome.toString());
        this.contextId = UUID.randomUUID().toString();
        this.imports = new ArrayList<>();
        this.moduleDclns = new ArrayList<>();
        this.globalVars = new HashMap<>();
    }

    @Override
    public void initialize() throws InvokerException {
        // Creates an empty context and loads the project.
        // This will allow compiler to cache necessary data so that
        // subsequent runs will be much more faster.
        ClassLoadContext emptyContext = new ClassLoadContext(contextId);
        SingleFileProject project = getProject(emptyContext);
        JBallerinaBackend loadedBackend = JBallerinaBackend.from(compile(project), JdkVersion.JAVA_11);
        execute(project, loadedBackend);
    }

    @Override
    public void reset() {
        // Clear everything in memory
        // data wrt the memory context is also removed.
        this.imports.clear();
        this.moduleDclns.clear();
        this.globalVars.clear();
        ClassLoadMemory.forgetAll(contextId);
    }

    @Override
    public boolean execute(Snippet newSnippet) throws InvokerException {
        List<Pair<String, String>> newVariables = new ArrayList<>();
        if (newSnippet.isVariableDeclaration()) {
            // This is a variable declaration.
            // So we have to compile once and know the names and types of variables.
            // The reason is some types (var) are determined at compile time.
            // Only compilation is done.
            VariableDeclarationSnippet varDcln = (VariableDeclarationSnippet) newSnippet;
            ClassLoadContext varTypeInferContext = createVarTypeInferContext(varDcln);
            SingleFileProject project = getProject(varTypeInferContext);
            PackageCompilation compilation = compile(project);

            for (BLangSimpleVariable variable : compilation.defaultModuleBLangPackage().getGlobalVariables()) {
                // If the variable is a init var or a known global var, add it.
                if (!INIT_VAR_NAMES.contains(variable.name.value) && !globalVars.containsKey(variable.name.value)) {
                    newVariables.add(new Pair<>(variable.name.value, variable.type.toString()));
                }
            }
        }

        // Compile and execute the real program.
        ClassLoadContext context = createContext(newSnippet, newVariables);
        SingleFileProject project = getProject(context);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);
        boolean isSuccess = execute(project, jBallerinaBackend);

        // Save required data if execution was successful
        if (isSuccess) {
            addDiagnostic(Diagnostic.debug("Adding the snippet to memory."));
            if (newSnippet.isImport()) {
                imports.add(newSnippet);
            } else if (newSnippet.isVariableDeclaration()) {
                newVariables.forEach(v -> globalVars.put(v.getFirst(), v.getSecond()));
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
        List<Pair<String, String>> initVarDclns = new ArrayList<>();
        List<Pair<String, String>> saveVarDclns = new ArrayList<>();
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));
        imports.stream().map(Objects::toString).forEach(importStrings::add);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        String lastVarDcln = newSnippet.toString();

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings,
                initVarDclns, saveVarDclns, lastVarDcln, null);
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
        // Variable declarations are handled differently.
        // If current snippet is a var dcln, it is added to saveVarDclns but not to initVarDclns.
        // All other var dclns are added to both.
        // Last expr is the last snippet if it was either a stmt or an expression.

        List<Pair<String, String>> initVarDclns = new ArrayList<>();
        List<Pair<String, String>> saveVarDclns = new ArrayList<>();
        List<String> importStrings = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>();
        imports.stream().map(Objects::toString).forEach(importStrings::add);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));

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

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings,
                initVarDclns, saveVarDclns, lastVarDcln, lastExpr);
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
            return invokeMethod(method) == 0;
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

    /**
     * Get the project with the context data.
     *
     * @param context Context to create the ballerina file.
     * @return Created ballerina project.
     * @throws InvokerException If file writing failed.
     */
    protected SingleFileProject getProject(Object context) throws InvokerException {
        this.template = Objects.requireNonNullElse(this.template, super.getTemplate(TEMPLATE_FILE));
        File mainBal = writeToFile(this.template, context);
        return SingleFileProject.load(mainBal.toPath());
    }

    /**
     * Runs a method given. Returns the exit code from the execution.
     * Method should be a static method which returns an int.
     * Its signature should be, {@code static int name(String[] args)}.
     * TODO: Catch errors and handle IO correctly.
     *
     * @param method Method to run (should be a static method).
     * @return Exit code of the method.
     * @throws IllegalAccessException If interrupted.
     */
    protected int invokeMethod(Method method) throws IllegalAccessException {
        String[] args = new String[0];

        PrintStream stdErr = System.err;
        PrintStream stdOut = System.out;
        ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            System.setOut(new PrintStream(stdOutBaOs));
            System.setSecurityManager(new NoExitVmSecManager(System.getSecurityManager()));
            return (int) method.invoke(null, new Object[]{args});
        } catch (InvocationTargetException ignored) {
            return 0;
        } finally {
            // Restore everything
            stdOut.print(new String(stdOutBaOs.toByteArray()));
            System.setSecurityManager(null);
            System.setErr(stdErr);
            System.setOut(stdOut);
        }
    }
}
