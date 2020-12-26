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
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.BuildOptionsBuilder;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JarResolver;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.exceptions.InvokerException;
import io.ballerina.shell.invoker.Invoker;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.types.ImportDeclarationSnippet;
import io.ballerina.shell.snippet.types.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.utils.Pair;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Executes the snippet given.
 * This invoker will save all the variable values in a static class and
 * load them into the generated class effectively managing any side-effects.
 */
public class ClassLoadInvoker extends Invoker {
    // Main class and method names to invoke
    protected static final String MODULE_INIT_CLASS_NAME = "$_init";
    protected static final String MODULE_MAIN_METHOD_NAME = "main";
    protected static final String EXPR_VAR_NAME = "expr";
    protected static final String CURSOR_NAME = "<<cursor>>";
    protected static final String DOLLAR = "$";
    // Initial context data
    protected static final Map<String, String> INITIAL_IMPORTS = Map.of("'java", "import ballerina/java;");
    protected static final Set<HashedSymbol> INITIALLY_KNOWN_SYMBOLS = HashedSymbol.defaults();
    // Punctuations
    private static final int MAX_VAR_STRING_LENGTH = 78;
    private static final String DECLARATION_TEMPLATE_FILE = "template.declaration.ftl";
    private static final String IMPORT_TEMPLATE_FILE = "template.import.ftl";
    private static final String TEMPLATE_FILE = "template.classload.ftl";

    /**
     * Set of symbols that are known or seen at this point.
     */
    protected final Set<HashedSymbol> knownSymbols;
    /**
     * The import creator to use in importing.
     */
    protected final TypeSignatureParser typeSignatureParser;
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
    protected final Map<String, String> moduleDclns;
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
    protected final Set<String> mustImportPrefixes;
    /**
     * Id of the current invoker context.
     */
    protected final String contextId;

    /**
     * Stores all the newly found symbols in this iteration.
     * This is reset in each iteration and is persisted to `knownSymbols` at the end
     * of the current iteration. (If it were a success)
     */
    private final Set<HashedSymbol> newSymbols;
    /**
     * Stores all the newly found implicit imports.
     * Persisted at the end of iteration to `mustImportPrefixes`.
     */
    private final Set<String> newImplicitImports;

    /**
     * Creates a class load invoker from the given ballerina home.
     * Ballerina home should be tha path that contains repo directory.
     * It is expected that the runtime is added in the class path.
     */
    public ClassLoadInvoker() {
        this.knownSymbols = new HashSet<>(INITIALLY_KNOWN_SYMBOLS);
        this.typeSignatureParser = new TypeSignatureParser(this::processImport);
        this.contextId = UUID.randomUUID().toString();
        this.imports = new HashMap<>();
        this.moduleDclns = new HashMap<>();
        this.globalVars = new HashMap<>();
        this.mustImportPrefixes = new HashSet<>();
        this.newSymbols = new HashSet<>();
        this.newImplicitImports = new HashSet<>();
    }

    /**
     * Creates an empty context and loads the project.
     * This will allow compiler to cache necessary data so that
     * subsequent runs will be much more faster.
     *
     * @throws InvokerException If initialization failed.
     */
    @Override
    public void initialize() throws InvokerException {
        ClassLoadContext emptyContext = new ClassLoadContext(contextId);
        SingleFileProject project = getProject(emptyContext, TEMPLATE_FILE);
        JBallerinaBackend loadedBackend = JBallerinaBackend.from(compile(project), JvmTarget.JAVA_11);
        executeProject(project, loadedBackend);
    }

    @Override
    public void reset() {
        // Clear everything in memory
        // data wrt the memory context is also removed.
        this.knownSymbols.clear();
        this.imports.clear();
        this.moduleDclns.clear();
        this.globalVars.clear();
        this.mustImportPrefixes.clear();
        ClassLoadMemory.forgetAll(contextId);
    }

    @Override
    public Pair<Boolean, Optional<Object>> execute(String source) throws InvokerException {
        // An alternative execute to directly execute a string source.
        SingleFileProject project = getProject(source);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JvmTarget.JAVA_11);
        boolean isSuccess = executeProject(project, jBallerinaBackend);
        return new Pair<>(isSuccess, null);
    }

    @Override
    public Pair<Boolean, Optional<Object>> execute(Snippet newSnippet) throws InvokerException {
        // New variables/dclns defined in this iteration.
        Map<String, String> newVariables = new HashMap<>();
        Pair<String, String> newModuleDcln = null;

        newSymbols.clear();
        newImplicitImports.clear();

        // TODO: Fix the closure bug. Following will not work with isolated functions.
        // newSnippet.modify(new GlobalLoadModifier(globalVars));

        if (newSnippet.isVariableDeclaration()) {
            assert newSnippet instanceof VariableDeclarationSnippet;
            VariableDeclarationSnippet varDcln = (VariableDeclarationSnippet) newSnippet;
            newVariables.putAll(processVarDcln(varDcln));
        } else if (newSnippet.isImport()) {
            // Only 1 compilation to find import validity and exit.
            // No execution is done.
            assert newSnippet instanceof ImportDeclarationSnippet;
            String importPrefix = processImport((ImportDeclarationSnippet) newSnippet);
            return new Pair<>(importPrefix != null, Optional.empty());
        } else if (newSnippet.isModuleMemberDeclaration()) {
            assert newSnippet instanceof ModuleMemberDeclarationSnippet;
            ModuleMemberDeclarationSnippet moduleDcln = (ModuleMemberDeclarationSnippet) newSnippet;
            newModuleDcln = processModuleDcln(moduleDcln);
        }

        // Compile and execute the real program.
        ClassLoadContext context = createContext(newSnippet, newVariables);
        SingleFileProject project = getProject(context, TEMPLATE_FILE);
        PackageCompilation compilation = compile(project);
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JvmTarget.JAVA_11);
        boolean isSuccess = executeProject(project, jBallerinaBackend);

        // Save required data if execution was successful
        if (isSuccess) {
            this.knownSymbols.addAll(newSymbols);
            this.mustImportPrefixes.addAll(newImplicitImports);
            if (newSnippet.isVariableDeclaration()) {
                newVariables.forEach(globalVars::put);
            } else if (newSnippet.isModuleMemberDeclaration()) {
                Objects.requireNonNull(newModuleDcln);
                moduleDclns.put(newModuleDcln.getFirst(), newModuleDcln.getSecond());
            }
        } else {
            addDiagnostic(Diagnostic.error("Unhandled Runtime Error."));
        }
        Object result = ClassLoadMemory.recall(contextId, EXPR_VAR_NAME);
        return new Pair<>(isSuccess, Optional.ofNullable(result));
    }

    /**
     * This is an import. A test import is done to check for errors.
     * It should not give 'module not found' error.
     * Only compilation is done to verify package resolution.
     *
     * @param importSnippet New import snippet string.
     * @return Whether import is a valid import.
     * @throws InvokerException If compilation failed.
     */
    private String processImport(ImportDeclarationSnippet importSnippet) throws InvokerException {
        String importString = importSnippet.toString();
        ClassLoadContext importCheckingContext = createImportInferContext(importString);
        SingleFileProject project = getProject(importCheckingContext, IMPORT_TEMPLATE_FILE);
        PackageCompilation compilation = project.currentPackage().getCompilation();

        // Detect if import is valid.
        for (io.ballerina.tools.diagnostics.Diagnostic diagnostic : compilation.diagnosticResult().diagnostics()) {
            if (diagnostic.diagnosticInfo().code().equals(DiagnosticErrorCode.MODULE_NOT_FOUND.diagnosticId())) {
                addDiagnostic(Diagnostic.error("Import resolution failed. Module not found."));
                return null;
            }
        }

        String importPrefix = importSnippet.getPrefix();
        if (INITIAL_IMPORTS.containsKey(importPrefix)) {
            addDiagnostic(Diagnostic.error("Import is already available by default."));
            return null;
        } else if (imports.containsKey(importPrefix)) {
            // TODO: Verify that this is the same import
            // use addDiagnostic(Diagnostic.error("An import was done before with the same prefix."));
            // return null;
            return importPrefix;
        }

        imports.put(importPrefix, importString);
        return importPrefix;
    }

    /**
     * Processes a variable declaration snippet.
     * We need to know all the variable types.
     * Some types (var) are determined at compile time.
     * So we have to compile once and know the names and types of variables.
     * Only compilation is done.
     *
     * @param newSnippet New variable declaration snippet.
     * @return Exported found variable information (name and type)
     * @throws InvokerException If type/name inferring failed.
     */
    private Map<String, String> processVarDcln(VariableDeclarationSnippet newSnippet) throws InvokerException {
        // No matter the approach, compile. This will confirm that syntax is valid.
        ClassLoadContext varTypeInferContext = createVarTypeInferContext(newSnippet);
        SingleFileProject project = getProject(varTypeInferContext, DECLARATION_TEMPLATE_FILE);
        Collection<Symbol> symbols = visibleUnknownSymbols(project);

        Map<String, String> foundVariables = new HashMap<>();
        for (Symbol symbol : symbols) {
            HashedSymbol hashedSymbol = new HashedSymbol(symbol);

            // Identify variable type
            TypeSymbol typeSymbol;
            if (symbol.kind() == SymbolKind.VARIABLE) {
                assert symbol instanceof VariableSymbol;
                typeSymbol = ((VariableSymbol) symbol).typeDescriptor();
            } else if (symbol.kind() == SymbolKind.FUNCTION) {
                assert symbol instanceof FunctionSymbol;
                typeSymbol = ((FunctionSymbol) symbol).typeDescriptor();
            } else {
                continue;
            }

            // Add variable type
            String variableName = symbol.name();
            if (knownSymbols.contains(hashedSymbol)
                    || foundVariables.containsKey(variableName)
                    || variableName.contains(DOLLAR)) {
                continue;
            }

            Pair<String, Set<String>> parsedData = typeSignatureParser.process(typeSymbol);
            foundVariables.put(symbol.name(), parsedData.getFirst());
            this.newImplicitImports.addAll(parsedData.getSecond());
            this.newSymbols.add(hashedSymbol);
        }

        return foundVariables;
    }

    /**
     * Processes a variable declaration snippet.
     * Symbols are processed to know the new module dcln.
     * Only compilation is done.
     * TODO: Support enums.
     *
     * @param newSnippet New snippet to process.
     * @return The newly found type name and its declaration.
     * @throws InvokerException If module dcln is invalid.
     */
    private Pair<String, String> processModuleDcln(ModuleMemberDeclarationSnippet newSnippet) throws InvokerException {
        // Add all required imports
        this.newImplicitImports.addAll(newSnippet.usedImports());

        ClassLoadContext varTypeInferContext = createDclnNameInferContext(newSnippet);
        SingleFileProject project = getProject(varTypeInferContext, DECLARATION_TEMPLATE_FILE);
        Collection<Symbol> symbols = visibleUnknownSymbols(project);

        for (Symbol symbol : symbols) {
            this.newSymbols.add(new HashedSymbol(symbol));
            return new Pair<>(symbol.name(), newSnippet.toString());
        }

        addDiagnostic(Diagnostic.error("Invalid module level declaration: cannot be compiled."));
        throw new InvokerException();
    }

    /**
     * Creates a context which can be used to check import validation.
     *
     * @param importString Import declaration snippet string.
     * @return Context with import checking code.
     */
    protected ClassLoadContext createImportInferContext(String importString) {
        return new ClassLoadContext(this.contextId, List.of(importString));
    }

    /**
     * Creates a context which can be used to identify new variables.
     *
     * @param newSnippet New snippet. Must be a var dcln.
     * @return Context with type information inferring code.
     */
    protected ClassLoadContext createVarTypeInferContext(VariableDeclarationSnippet newSnippet) {
        List<ClassLoadContext.Variable> varDclns = new ArrayList<>();
        globalVars.forEach((k, v) -> varDclns.add(ClassLoadContext.Variable.oldVar(k, v)));
        List<String> moduleDclnStrings = new ArrayList<>(moduleDclns.values());

        // Imports = snippet imports + module def imports
        Set<String> importStrings = getUsedImportStatements(newSnippet);
        mustImportPrefixes.stream().map(imports::get)
                .filter(Objects::nonNull).forEach(importStrings::add);

        String lastVarDcln = newSnippet.toString();

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings, varDclns, lastVarDcln);
    }

    /**
     * Creates a context which can be used to find declaration name.
     *
     * @param newSnippet New snippet. Must be a module member dcln.
     * @return Context to infer dcln name.
     */
    protected ClassLoadContext createDclnNameInferContext(ModuleMemberDeclarationSnippet newSnippet) {
        List<ClassLoadContext.Variable> varDclns = new ArrayList<>();
        globalVars.forEach((k, v) -> varDclns.add(ClassLoadContext.Variable.oldVar(k, v)));
        List<String> moduleDclnStrings = new ArrayList<>(moduleDclns.values());
        moduleDclnStrings.add(newSnippet.toString());

        // Get all required imports
        Set<String> importStrings = getUsedImportStatements(newSnippet);
        mustImportPrefixes.stream().map(imports::get)
                .filter(Objects::nonNull).forEach(importStrings::add);

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings, varDclns, null);
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

        List<ClassLoadContext.Variable> varDclns = new ArrayList<>();
        List<String> moduleDclnStrings = new ArrayList<>(moduleDclns.values());
        globalVars.forEach((k, v) -> varDclns.add(ClassLoadContext.Variable.oldVar(k, v)));

        // Imports = snippet imports + var def imports + module def imports
        Set<String> importStrings = getUsedImportStatements(newSnippet);
        mustImportPrefixes.stream().map(imports::get)
                .filter(Objects::nonNull).forEach(importStrings::add);

        Pair<String, Boolean> lastExpr = null;
        String lastVarDcln = null;
        if (newSnippet.isImport()) {
            importStrings.add(newSnippet.toString());
        } else if (newSnippet.isVariableDeclaration()) {
            lastVarDcln = newSnippet.toString();
            for (Map.Entry<String, String> entry : newVariables.entrySet()) {
                ClassLoadContext.Variable variable =
                        ClassLoadContext.Variable.newVar(entry.getKey(), entry.getValue());
                varDclns.add(variable);
            }
        } else if (newSnippet.isModuleMemberDeclaration()) {
            moduleDclnStrings.add(newSnippet.toString());
        } else if (newSnippet.isStatement()) {
            lastExpr = new Pair<>(newSnippet.toString(), true);
        } else if (newSnippet.isExpression()) {
            lastExpr = new Pair<>(newSnippet.toString(), false);
        }

        return new ClassLoadContext(this.contextId, importStrings, moduleDclnStrings, varDclns, lastVarDcln, lastExpr);
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
            addDiagnostic(Diagnostic.debug("Using ballerina source file: " + mainBal));
            BuildOptions buildOptions = new BuildOptionsBuilder().offline(true).build();
            return SingleFileProject.load(mainBal.toPath(), buildOptions);
        } catch (IOException e) {
            addDiagnostic(Diagnostic.error("File writing failed: " + e.getMessage()));
            throw new InvokerException(e);
        }
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
    protected boolean executeProject(Project project, JBallerinaBackend jBallerinaBackend) throws InvokerException {
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
     * Gets the symbols that are visible to main method but are unknown (previously not seen).
     *
     * @param project Project to get symbols.
     * @return All the visible symbols.
     */
    protected Collection<Symbol> visibleUnknownSymbols(Project project) throws InvokerException {
        PackageCompilation compilation = compile(project);

        // Get the document associated with project
        Module module = project.currentPackage().getDefaultModule();
        ModuleId moduleId = module.moduleId();
        Optional<DocumentId> documentId = module.documentIds().stream().findFirst();
        assert documentId.isPresent();
        Document document = module.document(documentId.get());

        // Find the position of cursor to find the symbols
        AtomicReference<LinePosition> reference = new AtomicReference<>();
        document.syntaxTree().rootNode().accept(new NodeVisitor() {
            @Override
            public void visit(Token token) {
                token.leadingMinutiae().forEach((m) -> {
                    if (m.text().contains(CURSOR_NAME)) {
                        reference.set(m.lineRange().startLine());
                    }
                });
            }
        });

        return compilation.getSemanticModel(moduleId)
                .visibleSymbols(document.name(), reference.get())
                .stream()
                .filter((s) -> !knownSymbols.contains(new HashedSymbol(s)))
                .collect(Collectors.toList());
    }

    /**
     * Return import strings used by this snippet.
     *
     * @param snippet Snippet to check.
     * @return List of imports.
     */
    protected Set<String> getUsedImportStatements(Snippet snippet) {
        Set<String> importStrings = new HashSet<>();
        snippet.usedImports().stream()
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

    // Available statements

    @Override
    public String availableImports() {
        // Imports with prefixes
        Map<String, String> importMapped = new HashMap<>(INITIAL_IMPORTS);
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
            String varString = String.format("(%s) %s %s = %s",
                    entry.getKey(), entry.getValue(), entry.getKey(), value);
            varStrings.add(varString);
        }
        return String.join("\n", varStrings);
    }

    @Override
    public String availableModuleDeclarations() {
        // Module level dclns.
        List<String> moduleDclnStrings = new ArrayList<>();
        for (Map.Entry<String, String> entry : moduleDclns.entrySet()) {
            String varString = String.format("(%s) %s", entry.getKey(),
                    shortenedString(entry.getValue()));
            moduleDclnStrings.add(varString);
        }
        return String.join("\n", moduleDclnStrings);
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
