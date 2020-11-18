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

package io.ballerina.shell.snippet;

import io.ballerina.compiler.syntax.tree.AnnotationDeclarationNode;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BreakStatementNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.ContinueStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ErrorBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ErrorTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.ListBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.LocalTypeDefinitionStatementNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MappingBindingPatternNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleXMLNamespaceDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgBindingPatternNode;
import io.ballerina.compiler.syntax.tree.NilTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.ParameterizedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RestBindingPatternNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SingletonTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TableConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.TableTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.TupleTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceTypeDescNode;
import io.ballerina.compiler.syntax.tree.TypeTestExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedescTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.compiler.syntax.tree.WildcardBindingPatternNode;
import io.ballerina.compiler.syntax.tree.XMLNamespaceDeclarationNode;
import io.ballerina.compiler.syntax.tree.XmlTypeDescriptorNode;
import io.ballerina.shell.PrinterProvider;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.snippet.types.ErroneousSnippet;
import io.ballerina.shell.snippet.types.ExpressionSnippet;
import io.ballerina.shell.snippet.types.ImportSnippet;
import io.ballerina.shell.snippet.types.ModuleMemberDeclarationSnippet;
import io.ballerina.shell.snippet.types.StatementSnippet;
import io.ballerina.shell.snippet.types.VariableDeclarationSnippet;
import io.ballerina.shell.snippet.types.VariableType;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to create snippets from nodes.
 */
public class SnippetFactory {
    private static final String RESERVED_PREFIX = "_";
    private static final Map<SyntaxKind, VariableType> TYPE_MAP = new HashMap<>();
    private static final Map<Class<?>, SnippetSubKind> MODULE_MEM_DCLNS = new HashMap<>();
    private static final Map<Class<?>, SnippetSubKind> STATEMENTS = new HashMap<>();
    private static final Map<Class<?>, SnippetSubKind> EXPRESSIONS = new HashMap<>();

    // Create a cache of Syntax kind -> Variable types/Sub snippets that are known.
    // This will be used when identifying the variable type/snippet type.
    static {
        TYPE_MAP.put(SyntaxKind.NIL_TYPE_DESC, VariableType.NIL);
        TYPE_MAP.put(SyntaxKind.INT_KEYWORD, VariableType.INT);
        TYPE_MAP.put(SyntaxKind.FLOAT_KEYWORD, VariableType.FLOAT);
        TYPE_MAP.put(SyntaxKind.BOOLEAN_KEYWORD, VariableType.BOOLEAN);
        TYPE_MAP.put(SyntaxKind.DECIMAL_KEYWORD, VariableType.DECIMAL);
        TYPE_MAP.put(SyntaxKind.STRING_KEYWORD, VariableType.STRING);
        TYPE_MAP.put(SyntaxKind.XML_TYPE_DESC, VariableType.XML);
        TYPE_MAP.put(SyntaxKind.ARRAY_TYPE_DESC, VariableType.ARRAY);
        TYPE_MAP.put(SyntaxKind.TUPLE_TYPE_DESC, VariableType.TUPLE);
        TYPE_MAP.put(SyntaxKind.JSON_KEYWORD, VariableType.JSON);
        TYPE_MAP.put(SyntaxKind.ANY_KEYWORD, VariableType.ANY);
        TYPE_MAP.put(SyntaxKind.OPTIONAL_TYPE_DESC, VariableType.OPTIONAL);
        TYPE_MAP.put(SyntaxKind.ANYDATA_KEYWORD, VariableType.ANYDATA);
        TYPE_MAP.put(SyntaxKind.BYTE_KEYWORD, VariableType.BYTE);
        TYPE_MAP.put(SyntaxKind.MAP_KEYWORD, VariableType.MAP);
        TYPE_MAP.put(SyntaxKind.FUTURE_KEYWORD, VariableType.FUTURE);
        TYPE_MAP.put(SyntaxKind.STREAM_KEYWORD, VariableType.STREAM);
        TYPE_MAP.put(SyntaxKind.HANDLE_KEYWORD, VariableType.HANDLE);
        TYPE_MAP.put(SyntaxKind.SERVICE_KEYWORD, VariableType.SERVICE);
        TYPE_MAP.put(SyntaxKind.NEVER_KEYWORD, VariableType.NEVER);
        MODULE_MEM_DCLNS.put(FunctionDefinitionNode.class, SnippetSubKind.FUNCTION_DEFINITION);
        MODULE_MEM_DCLNS.put(ListenerDeclarationNode.class, SnippetSubKind.LISTENER_DECLARATION);
        MODULE_MEM_DCLNS.put(TypeDefinitionNode.class, SnippetSubKind.TYPE_DEFINITION);
        MODULE_MEM_DCLNS.put(ServiceDeclarationNode.class, SnippetSubKind.SERVICE_DECLARATION);
        MODULE_MEM_DCLNS.put(ConstantDeclarationNode.class, SnippetSubKind.CONSTANT_DECLARATION);
        MODULE_MEM_DCLNS.put(ModuleVariableDeclarationNode.class, SnippetSubKind.MODULE_VARIABLE_DECLARATION);
        MODULE_MEM_DCLNS.put(AnnotationDeclarationNode.class, SnippetSubKind.ANNOTATION_DECLARATION);
        MODULE_MEM_DCLNS.put(ModuleXMLNamespaceDeclarationNode.class, SnippetSubKind.MODULE_XML_NAMESPACE_DECLARATION);
        MODULE_MEM_DCLNS.put(EnumDeclarationNode.class, SnippetSubKind.ENUM_DECLARATION);
        MODULE_MEM_DCLNS.put(ClassDefinitionNode.class, SnippetSubKind.CLASS_DEFINITION);
        STATEMENTS.put(AssignmentStatementNode.class, SnippetSubKind.ASSIGNMENT_STATEMENT_SUBKIND);
        STATEMENTS.put(CompoundAssignmentStatementNode.class, SnippetSubKind.COMPOUND_ASSIGNMENT_STATEMENT_SUBKIND);
        STATEMENTS.put(VariableDeclarationNode.class, SnippetSubKind.VARIABLE_DECLARATION_STATEMENT);
        STATEMENTS.put(BlockStatementNode.class, SnippetSubKind.BLOCK_STATEMENT);
        STATEMENTS.put(BreakStatementNode.class, SnippetSubKind.BREAK_STATEMENT);
        STATEMENTS.put(FailStatementNode.class, SnippetSubKind.FAIL_STATEMENT);
        STATEMENTS.put(ExpressionStatementNode.class, SnippetSubKind.EXPRESSION_STATEMENT);
        STATEMENTS.put(ContinueStatementNode.class, SnippetSubKind.CONTINUE_STATEMENT);
        STATEMENTS.put(IfElseStatementNode.class, SnippetSubKind.WHILE_STATEMENT);
        STATEMENTS.put(WhileStatementNode.class, SnippetSubKind.WHILE_STATEMENT);
        STATEMENTS.put(PanicStatementNode.class, SnippetSubKind.PANIC_STATEMENT);
        STATEMENTS.put(ReturnStatementNode.class, SnippetSubKind.RETURN_STATEMENT);
        STATEMENTS.put(LocalTypeDefinitionStatementNode.class, SnippetSubKind.LOCAL_TYPE_DEFINITION_STATEMENT);
        STATEMENTS.put(LockStatementNode.class, SnippetSubKind.LOCK_STATEMENT);
        STATEMENTS.put(ForkStatementNode.class, SnippetSubKind.FORK_STATEMENT);
        STATEMENTS.put(ForEachStatementNode.class, SnippetSubKind.FOR_EACH_STATEMENT);
        STATEMENTS.put(XMLNamespaceDeclarationNode.class, SnippetSubKind.XML_NAMESPACE_DECLARATION_STATEMENT);
        STATEMENTS.put(TransactionStatementNode.class, SnippetSubKind.TRANSACTION_STATEMENT);
        STATEMENTS.put(RollbackStatementNode.class, SnippetSubKind.ROLLBACK_STATEMENT);
        STATEMENTS.put(RetryStatementNode.class, SnippetSubKind.RETRY_STATEMENT);
        STATEMENTS.put(MatchStatementNode.class, SnippetSubKind.MATCH_STATEMENT);
        STATEMENTS.put(DoStatementNode.class, SnippetSubKind.DO_STATEMENT);
        EXPRESSIONS.put(TypeTestExpressionNode.class, SnippetSubKind.TYPE_TEST_EXPRESSION);
        EXPRESSIONS.put(TableConstructorExpressionNode.class, SnippetSubKind.TABLE_CONSTRUCTOR_EXPRESSION);
        EXPRESSIONS.put(ServiceConstructorExpressionNode.class, SnippetSubKind.SERVICE_CONSTRUCTOR_EXPRESSION);
    }

    private interface CreateSnippetFromNode {
        Snippet createSnippet() throws SnippetException;
    }

    /**
     * Creates a snippet from the given node.
     * This will throw and error if the resultant snippet is an erroneous snippet.
     */
    public Snippet fromNode(Node node) throws SnippetException {
        CreateSnippetFromNode[] creators = {
                () -> importFromNode(node),
                () -> variableDeclarationFromNode(node),
                () -> moduleMemberDeclarationFromNode(node),
                () -> expressionFromNode(node),
                () -> statementFromNode(node),
                () -> erroneousFromNode(node)
        };
        Snippet snippet;
        for (CreateSnippetFromNode creator : creators) {
            snippet = creator.createSnippet();
            if (snippet == null) {
                continue;
            }
            snippet.throwIfSnippetHasError();
            if (snippet.isIgnored()) {
                continue;
            }
            return snippet;
        }

        // Should not have reached here. But whatever.
        PrinterProvider.debug("Snippet parsing failed.");
        throw new SnippetException("Invalid syntax. Unknown snippet type.");
    }

    /**
     * Create a import snippet from the given node.
     * Returns null if snippet cannot be created.
     * Only the imports with a prefixes are allowed.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public ImportSnippet importFromNode(Node node) throws SnippetException {
        if (node instanceof ImportDeclarationNode) {
            ImportDeclarationNode importNode = ((ImportDeclarationNode) node);
            if (importNode.prefix().isEmpty()) {
                throw new SnippetException("" +
                        "Importing external modules without a prefix is restricted in REPL.\n" +
                        "Use a import prefix. Eg: import ballerina/io as bio;");
            }
            String importPrefix = importNode.prefix().get().prefix().text();
            return new ImportSnippet(node, importPrefix);
        }
        return null;
    }

    /**
     * Create a var definition snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public VariableDeclarationSnippet variableDeclarationFromNode(Node node) throws SnippetException {
        // Convert to module level or reject different nodes
        ModuleVariableDeclarationNode dclnNode = variableDeclarationToModuleVariable(node);
        if (dclnNode != null) {
            VariableType type = variableDeclarationGetType(dclnNode.typedBindingPattern().typeDescriptor());
            dclnNode = variableDeclarationInjectDefault(dclnNode, type);
            String variableName = variableDeclarationIdentifyName(dclnNode.typedBindingPattern().bindingPattern());
            if (variableName.startsWith(RESERVED_PREFIX)) {
                String message = String.format("No variable names may start with %s in the REPL.", RESERVED_PREFIX);
                throw new SnippetException(message);
            }
            String debugMessage = String.format("Identified variable name %s.", variableName);
            PrinterProvider.debug(debugMessage);
            return new VariableDeclarationSnippet(dclnNode, variableName, type.isSerializable());
        }
        return null;
    }

    /**
     * Create a module member declaration snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ModuleMemberDeclarationSnippet moduleMemberDeclarationFromNode(Node node) {
        if (MODULE_MEM_DCLNS.containsKey(node.getClass())) {
            SnippetSubKind subKind = MODULE_MEM_DCLNS.get(node.getClass());
            return new ModuleMemberDeclarationSnippet(node, subKind);
        }
        return null;
    }

    /**
     * Create a statement snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static StatementSnippet statementFromNode(Node node) {
        if (STATEMENTS.containsKey(node.getClass())) {
            SnippetSubKind subKind = STATEMENTS.get(node.getClass());
            return new StatementSnippet(node, subKind);
        }
        return null;
    }

    /**
     * Create a expression snippet from the given node.
     * Returns null if snippet cannot be created.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ExpressionSnippet expressionFromNode(Node node) {
        if (EXPRESSIONS.containsKey(node.getClass())) {
            SnippetSubKind subKind = EXPRESSIONS.get(node.getClass());
            return new ExpressionSnippet(node, subKind);
        } else if (node instanceof ExpressionNode) {
            return new ExpressionSnippet(node, SnippetSubKind.OTHER_EXPRESSION);
        }
        return null;
    }

    /**
     * Create an error snippet from the given node.
     *
     * @param node Root node to create snippet from.
     * @return Snippet that contains the node.
     */
    public static ErroneousSnippet erroneousFromNode(Node node) {
        return new ErroneousSnippet(node);
    }

    /**
     * Converts the node to a module level variable.
     * Initializer may be null in the resulting node.
     *
     * @param node Node to convert.
     * @return Module variable declaration node. Null if not valid.
     */
    protected ModuleVariableDeclarationNode variableDeclarationToModuleVariable(Node node) {
        // === Converting the node to a Module Level Declaration ===
        // Only variables that are allowed are module level variables.
        // So, all the variable declarations are converted into module
        // level variable declarations. However, there might be other
        // variable declarations as well. If that is the case attempt to
        // parse it as a module level variable by removing/adding some
        // additional information.

        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varDcln = (VariableDeclarationNode) node;
            return NodeFactory.createModuleVariableDeclarationNode(
                    NodeFactory.createMetadataNode(null, varDcln.annotations()),
                    varDcln.finalKeyword().orElse(null), varDcln.typedBindingPattern(),
                    varDcln.equalsToken().orElse(null), varDcln.initializer().orElse(null),
                    varDcln.semicolonToken()
            );
        } else if (node instanceof ModuleVariableDeclarationNode) {
            return (ModuleVariableDeclarationNode) node;
        } else {
            return null;
        }
    }

    /**
     * Identify the variable type associated with the type.
     *
     * @param type Node to get syntax kind of.
     * @return Kind that is associated with node. UNIDENTIFIED if failed.
     */
    protected VariableType variableDeclarationGetType(TypeDescriptorNode type) throws SnippetException {
        if (type instanceof ErrorTypeDescriptorNode) {
            return VariableType.ERROR.validated();
        } else if (type instanceof FunctionTypeDescriptorNode) {
            return VariableType.FUNCTION.validated();
        } else if (type instanceof StreamTypeDescriptorNode) {
            return VariableType.STREAM.validated();
        } else if (type instanceof ObjectTypeDescriptorNode) {
            return VariableType.OBJECT.validated();
        } else if (type instanceof TypedescTypeDescriptorNode) {
            return VariableType.TYPEDESC.validated();
        } else if (type instanceof SingletonTypeDescriptorNode) {
            return VariableType.SINGLETON.validated();
        } else if (type instanceof TableTypeDescriptorNode) {
            return VariableType.TABLE.validated();
        } else if (type instanceof XmlTypeDescriptorNode) {
            return VariableType.XML.validated();
        } else if (type instanceof RecordTypeDescriptorNode) {
            return VariableType.RECORD.validated();
        } else if (type instanceof ArrayTypeDescriptorNode) {
            return VariableType.ARRAY.validated();
        } else if (type instanceof TupleTypeDescriptorNode) {
            return VariableType.TUPLE.validated();
        } else if (type instanceof NilTypeDescriptorNode) {
            return VariableType.NIL.validated();
        } else if (type instanceof TypeReferenceTypeDescNode) {
            return variableDeclarationGetType(((TypeReferenceTypeDescNode) type).typeRef());
        } else if (type instanceof ParenthesisedTypeDescriptorNode) {
            return variableDeclarationGetType(((ParenthesisedTypeDescriptorNode) type).typedesc());
        } else if (type instanceof BuiltinSimpleNameReferenceNode) {
            return variableDeclarationTypeFromToken(((BuiltinSimpleNameReferenceNode) type).name());
        } else if (type instanceof SimpleNameReferenceNode) {
            return variableDeclarationTypeFromToken(((SimpleNameReferenceNode) type).name());
        } else if (type instanceof OptionalTypeDescriptorNode) {
            // Optional cannot be inferred directly
            // Serializable if all types serializable.
            // Default is ()
            Node typeDescriptor = ((OptionalTypeDescriptorNode) type).typeDescriptor();
            VariableType rest;
            if (typeDescriptor instanceof TypeDescriptorNode) {
                rest = variableDeclarationGetType((TypeDescriptorNode) typeDescriptor);
            } else if (typeDescriptor instanceof Token) {
                rest = variableDeclarationTypeFromToken((Token) typeDescriptor);
            } else {
                rest = VariableType.UNIDENTIFIED.validated();
            }
            ExpressionNode defaultType = rest.getDefaultValue().orElse(null);
            boolean isSerializable = VariableType.OPTIONAL.isSerializable() && rest.isSerializable();
            return new VariableType(isSerializable, defaultType);
        } else if (type instanceof UnionTypeDescriptorNode) {
            // Union cannot be inferred directly
            // Union default cannot be determined. We need internal types for that.
            //  Serializable if all types serializable.
            //  Default value of any type with a default.
            VariableType leftType = variableDeclarationGetType(((UnionTypeDescriptorNode) type).leftTypeDesc());
            VariableType rightType = variableDeclarationGetType(((UnionTypeDescriptorNode) type).rightTypeDesc());
            ExpressionNode defaultType = leftType.getDefaultValue().orElse(rightType.getDefaultValue().orElse(null));
            boolean isSerializable = VariableType.UNION.isSerializable()
                    && leftType.isSerializable() && rightType.isSerializable();
            return new VariableType(isSerializable, defaultType);
        } else if (type instanceof ParameterizedTypeDescriptorNode) {
            // Something like map<string>
            // Parameterized type as well as type parameter should be serializable
            // Default would be the default for parameterized type. (if any)
            // Eg: to be serializable, map and string should be serializable.
            //     default would be the default of map.
            ParameterizedTypeDescriptorNode paramTypeDes = (ParameterizedTypeDescriptorNode) type;
            VariableType paramType = variableDeclarationTypeFromToken(paramTypeDes.parameterizedType());
            VariableType typeParam = variableDeclarationGetType(paramTypeDes.typeParameter().typeNode());
            return new VariableType(typeParam.isSerializable() && paramType.isSerializable(),
                    paramType.getDefaultValue().orElse(null));
        } else if (type instanceof QualifiedNameReferenceNode
                || type instanceof IndexedExpressionNode
                || type instanceof TypeReferenceNode) {
            return VariableType.UNIDENTIFIED.validated();
        }
        return VariableType.UNIDENTIFIED.validated();
    }

    /**
     * Identify the variable type associated with the type.
     *
     * @param token Node to get syntax kind of.
     * @return Kind that is associated with node. UNIDENTIFIED if failed.
     */
    protected VariableType variableDeclarationTypeFromToken(Token token) throws SnippetException {
        return TYPE_MAP.getOrDefault(token.kind(), VariableType.UNIDENTIFIED).validated();
    }

    /**
     * Injects a default filler value to the module variable declaration.
     *
     * @param dclnNode Declaration node.
     * @param type     Type of the node.
     * @return Injected declaration node.
     */
    protected ModuleVariableDeclarationNode variableDeclarationInjectDefault
    (ModuleVariableDeclarationNode dclnNode, VariableType type) throws SnippetException {
        // === Inferring a default value ===
        // Also some declarations might not give a initializer.
        // These are not allowed in module level. Using type we can
        // infer a default value.
        // Find the type and try to infer the type.
        // There can be several types of variable declarations.
        // 1. int i = 0     -- has initializer, can infer default
        // 2. int i         -- no initializer, can infer default
        // 3. error i = f() -- has initializer, cant infer default
        // 4. error i       -- no initializer, cant infer default
        // In these ones, (1) and (3) does not need inferring.
        // (2) can be inferred. (4) is rejected.
        // So we need to infer only if the type is a doable type and
        // no initializer present.

        // Check if already has a default
        if (dclnNode.initializer().isPresent()) {
            return dclnNode;
        }

        // If inferring failed as well, throw an error message.
        if (type.getDefaultValue().isEmpty()) {
            throw new SnippetException("" +
                    "Initializer is required for variable declarations of this type.\n" +
                    "REPL will infer most of the types' default values, but this type could not be inferred.");
        }

        // Inject '= value' part.
        PrinterProvider.debug("Inferred default value: " + type.getDefaultValue().get());
        return dclnNode.modify()
                .withEqualsToken(NodeFactory.createToken(SyntaxKind.EQUAL_TOKEN))
                .withInitializer(type.getDefaultValue().get()).apply();
    }

    /**
     * Identifies tha variable name(s) that are defined in the declaration.
     * No identical names are defined in a binding pattern.
     *
     * @param bind Binding pattern enclosing the name.
     * @return Names that are defined in the statement.
     */
    protected String variableDeclarationIdentifyName(BindingPatternNode bind) throws SnippetException {
        // === Identify variable name ===
        // Now try to identify the variable name.
        // We can use the binding pattern to identify the name.
        // In this stage any variable starting with RESERVED_PREFIX is rejected.
        //
        // Possible binds are,
        //   capture-binding-pattern, wildcard-binding-pattern, list-binding-pattern,
        //   mapping-binding-pattern, error-binding-pattern
        // However, mapping-binding-pattern and list-binding-pattern cannot be declared globally.
        // These will be disabled in the REPL.
        // error-binding-pattern will bind values to identifiers inside the error type.
        // So, these are not globally done.
        // TODO: Verify this statement.
        // wildcard-binding-pattern will not define any new variable name.
        // capture-binding-pattern are the only valid pattern.
        // Additionally rest-binding-pattern/named-arg-binding-pattern are also candidates
        // for binding pattern. But these cannot exist without map/list binding patterns.
        // So they are unexpected.
        // error-binding-pattern or wildcard-binding-pattern can appear on module level.
        // But these should be taken as statements instead.
        // TODO: Is this correct?

        if (bind instanceof CaptureBindingPatternNode) {
            return ((CaptureBindingPatternNode) bind).variableName().text();
        } else if (bind instanceof ErrorBindingPatternNode || bind instanceof WildcardBindingPatternNode) {
            throw new SnippetException("Invalid wild-card/error binding pattern.");
        } else if (bind instanceof MappingBindingPatternNode) {
            throw new SnippetException("" +
                    "Map/Record bindings are disabled in module level.\n" +
                    "Declarations of format Record {a:p, b:q} = {p:x,q:y} cannot be done in global level.");
        } else if (bind instanceof ListBindingPatternNode) {
            throw new SnippetException("" +
                    "List bindings are disabled in module level.\n" +
                    "Declarations of format [int,int] [a,b] = [1,0] cannot be done in global level.");
        } else if (bind instanceof NamedArgBindingPatternNode || bind instanceof RestBindingPatternNode) {
            throw new SnippetException("" +
                    "Unexpected binding pattern found.\n" +
                    "Please check your statement for syntax errors.");
        } else {
            throw new SnippetException("Unknown variable bind: " + bind.getClass().getSimpleName());
        }
    }
}
