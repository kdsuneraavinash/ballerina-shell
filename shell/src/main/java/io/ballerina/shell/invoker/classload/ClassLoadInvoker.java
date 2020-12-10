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
import freemarker.template.TemplateException;
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
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.Pair;
import org.ballerinalang.model.Name;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    /**
     * List of imports done. These are imported to the read generated code as necessary.
     * This is a map of import prefix to the import statement used.
     * Import prefix must be a quoted identifier.
     */
    protected final Map<String, String> imports;

    /**
     * List of module level declarations such as functions, classes, etc...
     * The snippets are saved as is.
     */
    protected final List<Snippet> moduleDclns;

    /**
     * List of global variables used in the code.
     * This is a map of variable name to its type.
     * The variable name must be a quoted identifier.
     */
    protected final Map<String, String> globalVars;

    /**
     * Imports that should be done regardless of usage in the current snippet.
     * These are possibly the imports that are done previously
     * in module level declarations or variable declarations.
     */
    protected final Set<String> mustImports;

    /**
     * Id of the current invoker context.
     */
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
        this.mustImports = new HashSet<>();
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
        this.mustImports.clear();
        ClassLoadMemory.forgetAll(contextId);
    }

    @Override
    public Pair<Boolean, Optional<Object>> execute(String source) throws InvokerException {
        // An alternative execute to directly execute a string source.
        SingleFileProject project = getProject(source);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);
        boolean isSuccess = execute(project, jBallerinaBackend);
        return new Pair<>(isSuccess, null);
    }

    @Override
    public Pair<Boolean, Optional<Object>> execute(Snippet newSnippet) throws InvokerException {
        // New variables defined in this iteration.
        Map<String, String> newVariables = new HashMap<>();
        // Imports that should be persisted to the next iteration.
        Set<String> persistImports = new HashSet<>();

        // TODO: Fix the closure bug. Following will not work with isolated functions.
        // newSnippet.modify(new GlobalLoadModifier(globalVars));

        if (newSnippet.isVariableDeclaration()) {
            assert newSnippet instanceof VariableDeclarationSnippet;
            VariableDeclarationSnippet varDcln = (VariableDeclarationSnippet) newSnippet;
            processVariableDeclaration(varDcln, newVariables, persistImports);
        } else if (newSnippet.isImport()) {
            String importPrefix = processImport(newSnippet.toString());
            return new Pair<>(importPrefix != null, Optional.empty());
        } else if (newSnippet.isModuleMemberDeclaration()) {
            newSnippet.usedImports().stream().map(this::quotedIdentifier)
                    .forEach(persistImports::add);
        }

        // Compile and execute the real program.
        ClassLoadContext context = createContext(newSnippet, newVariables);
        SingleFileProject project = getProject(context, TEMPLATE_FILE);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JdkVersion.JAVA_11);
        boolean isSuccess = execute(project, jBallerinaBackend);

        // Save required data if execution was successful
        if (isSuccess) {
            this.mustImports.addAll(persistImports);
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

    /**
     * Processes a variable declaration snippet.
     * We need to know all the variable types.
     * Some types (var) are determined at compile time.
     * So we have to compile once and know the names and types of variables.
     * Only compilation is done.
     * 1. First try to find type using syntax tree.
     * 2. If not, try to infer type straight from compilation.
     * 3. Import required imports to define types if needed.
     *
     * @param newSnippet     New variable declaration snippet.
     * @param foundVariables Map to add extracted data to.
     * @param foundImports   Set to add imports that should be persisted.
     * @throws InvokerException If type/name inferring failed.
     */
    private void processVariableDeclaration(VariableDeclarationSnippet newSnippet, Map<String, String> foundVariables,
                                            Set<String> foundImports) throws InvokerException {

        // Infer types of possible variables using the syntax tree.
        // TODO: Remove syntax tree type finding and improve latter methodology
        List<Pair<String, String>> declaredVarNames = newSnippet.findVariableNamesAndTypes(this);
        if (!declaredVarNames.isEmpty()) {
            declaredVarNames.forEach(p -> foundVariables.put(quotedIdentifier(p.getFirst()), p.getSecond()));
            // Imports required for the var dcln.
            newSnippet.withoutInitializer().usedImports().stream()
                    .map(this::quotedIdentifier).forEach(foundImports::add);
            return;
        }

        // If cannot be directly inferred, compile once and find the type.
        ClassLoadContext varTypeInferContext = createVarTypeInferContext(newSnippet);
        SingleFileProject project = getProject(varTypeInferContext, VAR_TYPE_TEMPLATE_FILE);
        PackageCompilation compilation = compile(project);

        for (BLangSimpleVariable variable : compilation.defaultModuleBLangPackage().getGlobalVariables()) {
            // If the variable is not a init var or a known global var, add it.
            String variableName = quotedIdentifier(variable.name.value);
            if (!INIT_VAR_NAMES.contains(variableName) && !globalVars.containsKey(variableName)
                    && !foundVariables.containsKey(variableName)) {
                if (variable.type.getKind().equals(TypeKind.ERROR)) {
                    // Then we need to infer the type and find the imports
                    // that are required for the inferred type.
                    Name type = variable.type.tsymbol.name;
                    Pair<String, String> importStatement = createImportForVarType(variable.type.tsymbol.pkgID, type);
                    foundVariables.put(variableName, importStatement.getSecond());
                    foundImports.add(importStatement.getFirst());
                } else {
                    // Declared without var, then the user
                    // must already have done required imports.
                    foundVariables.put(variableName, variable.type.toString());
                }
            }
        }
    }

    /**
     * This is an import. A test import is done to check for errors.
     * It should not give 'module not found' error.
     * Only compilation is done to verify package resolution.
     *
     * @param importString New import snippet string.
     * @return Whether import is a valid import.
     * @throws InvokerException If compilation failed.
     */
    private String processImport(String importString) throws InvokerException {
        ClassLoadContext importCheckingContext = createImportCheckingContext(importString);
        SingleFileProject project = getProject(importCheckingContext, IMPORT_TEMPLATE_FILE);
        PackageCompilation compilation = project.currentPackage().getCompilation();
        for (io.ballerina.tools.diagnostics.Diagnostic diagnostic :
                compilation.diagnosticResult().diagnostics()) {
            if (diagnostic.diagnosticInfo().code()
                    .equals(DiagnosticErrorCode.MODULE_NOT_FOUND.diagnosticId())) {
                addDiagnostic(Diagnostic.error("Import resolution failed. Module not found."));
                return null;
            }
        }
        // No imports are actually done. Not possible for a valid import.
        if (compilation.defaultModuleBLangPackage().imports.isEmpty()) {
            addDiagnostic(Diagnostic.error("Not a valid import statement."));
            return null;
        }
        BLangImportPackage importPackage = compilation.defaultModuleBLangPackage().imports.get(0);
        String importPrefix = quotedIdentifier(importPackage.alias.value);
        if (INIT_IMPORTS.containsKey(importPrefix)) {
            addDiagnostic(Diagnostic.error("Import is already available by default."));
            return null;
        } else if (imports.containsKey(importPrefix)) {
            return importPrefix;
        }
        imports.put(importPrefix, importString);
        return importPrefix;
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
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);

        // Imports = snippet imports + module def imports
        Set<String> importStrings = getUsedImportStrings(newSnippet);
        mustImports.stream().map(imports::get)
                .filter(Objects::nonNull).forEach(importStrings::add);

        String lastVarDcln = newSnippet.toString();

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings,
                initVarDclns, saveVarDclns, lastVarDcln, null);
    }

    /**
     * Creates a context which can be used to check import validation.
     *
     * @param importString Import declaration snippet string.
     * @return Context with import checking code.
     */
    protected ClassLoadContext createImportCheckingContext(String importString) {
        return new ClassLoadContext(this.contextId, List.of(importString), List.of(),
                List.of(), List.of(), null, null);
    }

    /**
     * Creates an quoted identifier to use for variable names.
     * This will allow quoted identifiers as well as unquoted ones to be
     * used in the context.
     *
     * @param rawIdentifier Identifier without quote.
     *                      (This can be any object, string representation is taken)
     * @return Quoted identifier.
     */
    protected String quotedIdentifier(Object rawIdentifier) {
        if (String.valueOf(rawIdentifier).startsWith(QUOTE)) {
            return String.valueOf(rawIdentifier);
        }
        return QUOTE + rawIdentifier;
    }

    /**
     * Get a possible unused identifier to import the package given.
     *
     * @param packageID Package ID to lookup import.
     * @param type      Type from import to use.
     * @return The import prefix and the type for the import.
     */
    private Pair<String, String> createImportForVarType(PackageID packageID, Name type) throws InvokerException {
        // TODO: Add version too?
        String importStatement = String.format("import %s/%s;", packageID.orgName, packageID.name);
        String importPrefix = processImport(importStatement);
        if (importPrefix == null) {
            throw new InvokerException();
        }
        String varType = String.format("%s:%s", importPrefix, quotedIdentifier(type));
        return new Pair<>(importPrefix, varType);
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
        moduleDclns.stream().map(Objects::toString).forEach(moduleDclnStrings::add);
        globalVars.forEach((k, v) -> initVarDclns.add(new Pair<>(k, v)));
        globalVars.forEach((k, v) -> saveVarDclns.add(new Pair<>(k, v)));

        // Imports = snippet imports + var def imports + module def imports
        Set<String> importStrings = getUsedImportStrings(newSnippet);
        mustImports.stream().map(imports::get)
                .filter(Objects::nonNull).forEach(importStrings::add);

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
        try (StringWriter stringWriter = new StringWriter()) {
            template.process(context, stringWriter);
            return getProject(stringWriter.toString());
        } catch (TemplateException e) {
            addDiagnostic(Diagnostic.error("Template processing failed: " + e.getMessage()));
            throw new InvokerException(e);
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("File generation failed: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }

    /**
     * Get the project with the context data.
     *
     * @param source Source to use for generating project.
     * @return Created ballerina project.
     * @throws InvokerException If file writing failed.
     */
    protected SingleFileProject getProject(String source) throws InvokerException {
        try {
            File mainBal = writeToFile(source);
            addDiagnostic(Diagnostic.debug("Using main file: " + mainBal));
            BuildOptions buildOptions = new BuildOptionsBuilder().offline(true).build();
            return SingleFileProject.load(mainBal.toPath(), buildOptions);
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("File writing failed: " + e.getMessage()));
            throw new InvokerException(e);
        }
    }

    /**
     * Return import strings used by this snippet.
     *
     * @param snippet Snippet to check.
     * @return List of imports.
     */
    protected Set<String> getUsedImportStrings(Snippet snippet) {
        Set<String> importStrings = new HashSet<>();
        snippet.usedImports().stream()
                .map(this::quotedIdentifier)
                .map(imports::get).filter(Objects::nonNull)
                .forEach(importStrings::add);
        // Process current snippet, module dclns and indirect imports
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

    @Override
    public String availableImports() {
        // Imports with prefixes
        Map<String, String> importMapped = new HashMap<>(INIT_IMPORTS);
        for (Map.Entry<String, String> entry : imports.entrySet()) {
            importMapped.put(entry.getKey(), entry.getValue());
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
}
