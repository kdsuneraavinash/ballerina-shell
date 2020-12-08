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
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.BuildOptionsBuilder;
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
import io.ballerina.shell.snippet.types.ImportDeclarationSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.Pair;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Executes the snippet given.
 * This invoker will save all the variable values in a static class and
 * load them into the generated class effectively managing any side-effects.
 */
public class ClassLoadInvoker extends Invoker {
    private static final int MAX_VAR_STRING_LENGTH = 78;
    private static final String VAR_TYPE_TEMPLATE_FILE = "template.type.ftl";
    private static final String IMPORT_TEMPLATE_FILE = "template.import.ftl";
    private static final String TEMPLATE_FILE = "template.classload.ftl";
    // Main class and method names to invoke
    protected static final String MODULE_INIT_CLASS_NAME = "$_init";
    protected static final String MODULE_MAIN_METHOD_NAME = "main";
    protected static final String EXPR_VAR_NAME = "expr";
    // Variables that are set from the start. These should not be cached.
    protected static final Map<String, String> INIT_IMPORTS = Map.of(
            "'io", "import ballerina/io;",
            "'java", "import ballerina/java;");
    protected static final Set<String> INIT_VAR_NAMES = Set.of("'context_id", "'$annotation_data");
    private static final String QUOTE = "'";

    protected final Map<String, Snippet> imports;
    protected final List<Snippet> moduleDclns;
    protected final Map<String, String> globalVars;
    protected final String contextId;

    /**
     * Creates a class load invoker from the given ballerina home.
     * Ballerina home should be tha path that contains repo directory.
     * It is expected that the runtime is added in the class path.
     */
    public ClassLoadInvoker() {
        this.contextId = UUID.randomUUID().toString();
        this.imports = new HashMap<>();
        this.moduleDclns = new ArrayList<>();
        this.globalVars = new HashMap<>();
    }

    @Override
    public void initialize() throws InvokerException {
        // Creates an empty context and loads the project.
        // This will allow compiler to cache necessary data so that
        // subsequent runs will be much more faster.
        ClassLoadContext emptyContext = new ClassLoadContext(contextId);
        SingleFileProject project = getProject(emptyContext, TEMPLATE_FILE);
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
    public Pair<Boolean, Optional<Object>> execute(Snippet newSnippet) throws InvokerException {
        Map<String, String> newVariables = new HashMap<>();

        // TODO: Fix the closure bug. Following will not work with isolated functions.
        // newSnippet.modify(new GlobalLoadModifier(globalVars));

        if (newSnippet.isVariableDeclaration()) {
            assert newSnippet instanceof VariableDeclarationSnippet;

            // If the type can be inferred without compiling, do so.
            ((VariableDeclarationSnippet) newSnippet).findVariableNamesAndTypes()
                    .forEach(p -> newVariables.put(quotedIdentifier(p.getFirst()), p.getSecond()));

            // This is a variable declaration.
            // So we have to compile once and know the names and types of variables.
            // The reason is some types (var) are determined at compile time.
            // Only compilation is done.
            VariableDeclarationSnippet varDcln = (VariableDeclarationSnippet) newSnippet;
            ClassLoadContext varTypeInferContext = createVarTypeInferContext(varDcln);
            SingleFileProject project = getProject(varTypeInferContext, VAR_TYPE_TEMPLATE_FILE);
            PackageCompilation compilation = compile(project);

            for (BLangSimpleVariable variable : compilation.defaultModuleBLangPackage().getGlobalVariables()) {
                // If the variable is a init var or a known global var, add it.
                String variableName = quotedIdentifier(variable.name.value);
                if (!INIT_VAR_NAMES.contains(variableName)
                        && !globalVars.containsKey(variableName)
                        && !newVariables.containsKey(variableName)) {
                    newVariables.put(variableName, variable.type.toString());
                }
            }
        } else if (newSnippet.isImport()) {
            // This is an import. A test import is done to check for errors.
            // It should not give 'module not found' error.
            // Only compilation is done to verify package resolution.
            assert newSnippet instanceof ImportDeclarationSnippet;
            ImportDeclarationSnippet importDcln = (ImportDeclarationSnippet) newSnippet;

            ClassLoadContext importCheckingContext = createImportCheckingContext(importDcln);
            SingleFileProject project = getProject(importCheckingContext, IMPORT_TEMPLATE_FILE);
            PackageCompilation compilation = project.currentPackage().getCompilation();
            for (io.ballerina.tools.diagnostics.Diagnostic diagnostic :
                    compilation.diagnosticResult().diagnostics()) {
                if (diagnostic.diagnosticInfo().code()
                        .equals(DiagnosticErrorCode.MODULE_NOT_FOUND.diagnosticId())) {
                    addDiagnostic(Diagnostic.error("Import resolution failed. Module not found."));
                    return new Pair<>(false, Optional.empty());
                }
            }
            if (compilation.defaultModuleBLangPackage().imports.isEmpty()) {
                addDiagnostic(Diagnostic.error("Not a valid import statement."));
                return new Pair<>(false, Optional.empty());
            }
            BLangImportPackage importPackage = compilation.defaultModuleBLangPackage().imports.get(0);
            String importPrefix = quotedIdentifier(importPackage.alias.value);
            if (INIT_IMPORTS.containsKey(importPrefix)) {
                addDiagnostic(Diagnostic.error("Import is already available by default."));
                return new Pair<>(false, Optional.empty());
            } else if (imports.containsKey(importPrefix)) {
                addDiagnostic(Diagnostic.error("A module was previously imported with the same prefix."));
                return new Pair<>(false, Optional.empty());
            }
            imports.put(importPrefix, importDcln);
            return new Pair<>(true, Optional.empty());
        }

        // Compile and execute the real program.
        ClassLoadContext context = createContext(newSnippet, newVariables);
        SingleFileProject project = getProject(context, TEMPLATE_FILE);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);
        boolean isSuccess = execute(project, jBallerinaBackend);

        // Save required data if execution was successful
        if (isSuccess) {
            addDiagnostic(Diagnostic.debug("Adding the snippet to memory."));
            if (newSnippet.isVariableDeclaration()) {
                newVariables.forEach(globalVars::put);
            } else if (newSnippet.isModuleMemberDeclaration()) {
                moduleDclns.add(newSnippet);
            }
        } else {
            addDiagnostic(Diagnostic.error("Unhandled Runtime Error."));
        }
        Object result = ClassLoadMemory.recall(contextId, EXPR_VAR_NAME);
        return new Pair<>(isSuccess, Optional.ofNullable(result));
    }

    @Override
    public String availableImports() {
        // Imports with prefixes
        Map<String, String> importMapped = new HashMap<>(INIT_IMPORTS);
        for (Map.Entry<String, Snippet> entry : imports.entrySet()) {
            importMapped.put(entry.getKey(), entry.getValue().toString());
        }
        List<String> importStrings = new ArrayList<>(importMapped.values());
        return String.join("\n", importStrings);
    }

    @Override
    public String availableVariables() {
        // Available variables and values as string.
        List<String> varStrings = new ArrayList<>();
        for (Map.Entry<String, String> entry : globalVars.entrySet()) {
            String value = shortenedString(ClassLoadMemory.recall(contextId, entry.getKey()));
            String varString = String.format("%s %s = %s", entry.getValue(), entry.getKey(), value);
            varStrings.add(varString);
        }
        return String.join("\n", varStrings);
    }

    @Override
    public String availableModuleDeclarations() {
        // Module level dclns.
        return moduleDclns.stream().map(this::shortenedString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Short a string to a certain length.
     *
     * @param input Input string to shorten.
     * @return Shortened string.
     */
    private String shortenedString(Object input) {
        String value = String.valueOf(input);
        value = value.replaceAll("\n", "");
        if (value.length() > MAX_VAR_STRING_LENGTH) {
            int subStrLength = MAX_VAR_STRING_LENGTH / 2;
            return value.substring(0, subStrLength)
                    + "..." + value.substring(value.length() - subStrLength);
        }
        return value;
    }

    /**
     * Creates a context which can be used to identify new variables.
     *
     * @param newSnippet New snippet. Must be a var dcln.
     * @return Context with type information inferring code.
     */
    protected ClassLoadContext createVarTypeInferContext(VariableDeclarationSnippet newSnippet) {
        List<String> moduleDclnStrings = new ArrayList<>();
        List<Pair<String, String>> initVarDclns = new ArrayList<>();
        List<Pair<String, String>> saveVarDclns = new ArrayList<>();
        List<String> importStrings = getUsedImportStrings(newSnippet);
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);

        String lastVarDcln = newSnippet.toString();

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings,
                initVarDclns, saveVarDclns, lastVarDcln, null);
    }

    /**
     * Creates a context which can be used to check import validation.
     *
     * @param newSnippet New snippet. Must be a import dcln.
     * @return Context with import checking code.
     */
    protected ClassLoadContext createImportCheckingContext(ImportDeclarationSnippet newSnippet) {
        return new ClassLoadContext(this.contextId, List.of(newSnippet.toString()), List.of(),
                List.of(), List.of(), null, null);
    }

    /**
     * Creates an quoted identifier to use for variable names.
     * This will allow quoted identifiers as well as unquoted ones to be
     * used in the context.
     *
     * @param rawIdentifier Identifier without quote.
     * @return Quoted identifier.
     */
    protected String quotedIdentifier(String rawIdentifier) {
        if (rawIdentifier.startsWith(QUOTE)) {
            return rawIdentifier;
        }
        return QUOTE + rawIdentifier;
    }

    /**
     * Creates the context object to be passed to template.
     * The new snippets are not added here. Instead they are added to copies.
     *
     * @param newSnippet   New snippet from user.
     * @param newVariables Newly defined variables. Must be set if snippet is a var dcln.
     * @return Created context.
     */
    protected ClassLoadContext createContext(Snippet newSnippet, Map<String, String> newVariables) {
        // Variable declarations are handled differently.
        // If current snippet is a var dcln, it is added to saveVarDclns but not to initVarDclns.
        // All other var dclns are added to both.
        // Last expr is the last snippet if it was either a stmt or an expression.

        List<Pair<String, String>> initVarDclns = new ArrayList<>();
        List<Pair<String, String>> saveVarDclns = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>();
        List<String> importStrings = getUsedImportStrings(newSnippet);
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));

        Pair<String, Boolean> lastExpr = null;
        String lastVarDcln = null;
        if (newSnippet.isImport()) {
            importStrings.add(newSnippet.toString());
        } else if (newSnippet.isVariableDeclaration()) {
            lastVarDcln = newSnippet.toString();
            newVariables.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));
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
     * Due to ballerina calling system.exit(), we need to disable these calls and
     * remove system error logs as well.
     *
     * @param project           Project to run.
     * @param jBallerinaBackend Backed to use.
     * @return Whether process execution was successful.
     * @throws InvokerException If execution failed.
     */
    protected boolean execute(Project project, JBallerinaBackend jBallerinaBackend) throws InvokerException {
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
            int exitCode = invokeMethod(method);
            addDiagnostic(Diagnostic.debug("Exit code was " + exitCode));
            return exitCode == 0;
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
     * @param context      Context to create the ballerina file.
     * @param templateFile Template file to load.
     * @return Created ballerina project.
     * @throws InvokerException If file writing failed.
     */
    protected SingleFileProject getProject(Object context, String templateFile) throws InvokerException {
        Template template = super.getTemplate(templateFile);
        File mainBal = writeToFile(template, context);
        addDiagnostic(Diagnostic.debug("Using main file: " + mainBal));
        BuildOptions buildOptions = new BuildOptionsBuilder().offline(true).build();
        return SingleFileProject.load(mainBal.toPath(), buildOptions);
    }

    /**
     * Return import strings used by this snippet.
     *
     * @param snippet Snippet to check.
     * @return List of imports.
     */
    protected List<String> getUsedImportStrings(Snippet snippet) {
        List<String> importStrings = new ArrayList<>();
        snippet.usedImports().stream().map(this::quotedIdentifier)
                .map(imports::get).filter(Objects::nonNull)
                .map(Snippet::toString).forEach(importStrings::add);
        return importStrings;
    }

    /**
     * Runs a method given. Returns the exit code from the execution.
     * Method should be a static method which returns an int.
     * Its signature should be, {@code static int name(String[] args)}.
     *
     * @param method Method to run (should be a static method).
     * @return Exit code of the method.
     * @throws IllegalAccessException If interrupted.
     */
    protected int invokeMethod(Method method) throws IllegalAccessException {
        String[] args = new String[0];

        // STDERR is completely ignored because Security Exceptions are thrown
        // So real errors will not be visible via STDERR.
        // Security manager is set to stop VM exits.

        PrintStream stdErr = System.err;
        NoExitVmSecManager secManager = new NoExitVmSecManager(System.getSecurityManager());
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream(), true, Charset.defaultCharset()));
            System.setSecurityManager(secManager);
            return (int) method.invoke(null, new Object[]{args});
        } catch (InvocationTargetException e) {
            return secManager.getExitCode();
        } finally {
            System.setSecurityManager(null);
            System.setErr(stdErr);
        }
    }
}
