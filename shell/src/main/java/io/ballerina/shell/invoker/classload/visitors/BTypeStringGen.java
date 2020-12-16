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

package io.ballerina.shell.invoker.classload.visitors;

import io.ballerina.shell.exceptions.InvokerException;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnnotationType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnyType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnydataType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BBuiltInRefType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFiniteType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFutureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BHandleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BIntersectionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BJSONType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNeverType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNilType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNoType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BPackageType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BParameterizedType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BServiceType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStreamType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStructureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTupleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTypedescType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BXMLType;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.util.Flags;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts a type into its string format.
 * Any imports that need to be done are also calculated.
 * Eg: if the type was abc/z:TypeA then it will be converted as
 * 'z:TypeA' and 'import abc/z' will be added as an import.
 * We need to traverse all the sub-typed because any sub-type
 * may need to be imported.
 * TODO: Currently do not support exported sub-types in object/record.
 * Eg: object {abc/c:P p;}. These situations are rare since most of the time,
 * the object or record is exported outright instead of specifying sub-types.
 */
public class BTypeStringGen extends BTypeTransformer<String> {
    protected static final Pattern IMP_WITH_VER_PATTERN = Pattern.compile("(.*)/(.*):[0-9.]*:(.*)");
    protected static final Pattern IMP_WITHOUT_VER_PATTERN = Pattern.compile("(.*):(.*)");
    protected static final String QUALIFIED_NAME_SEP = ":";
    private static final String QUOTE = "'";
    private final Set<String> foundImports;
    private final ImportCreator importCreator;

    public BTypeStringGen(Set<String> foundImports, ImportCreator importCreator) {
        this.foundImports = foundImports;
        this.importCreator = importCreator;
    }

    @Override
    public void visit(BType bType) {
        setState(getImportedName(bType, bType.toString()));
    }

    @Override
    public void visit(BMapType bMapType) {
        // String representation is map<CONSTRAINT>.
        // Add readonly flag if necessary.
        String stringRepr = transform(bMapType.constraint);
        stringRepr = "map<" + stringRepr + ">";
        setState(withReadOnly(bMapType, stringRepr));
    }

    @Override
    public void visit(BUnionType bUnionType) {
        // Simply add all sub types separated by |.
        // Add a ? at the end if nullable.
        Set<String> typeStrings = new HashSet<>();
        for (BType memberType : bUnionType.getMemberTypes()) {
            typeStrings.add(transform(memberType));
        }
        String stringRepr = "(" + String.join("|", typeStrings) + ")";
        if (bUnionType.isNullable()) {
            stringRepr = stringRepr + "?";
        }
        setState(stringRepr);
    }

    @Override
    public void visit(BArrayType bArrayType) {
        // BArrayType string is always in format 'eType[.....]'
        // And there are no types between [].
        // So we can convert to string, and replace first 'eType' part.
        String original = bArrayType.toString();
        String originalETypeStr = bArrayType.eType.toString();
        String eTypeStr = transform(bArrayType.eType);
        setState(eTypeStr + original.substring(originalETypeStr.length()));
    }

    @Override
    public void visit(BTableType bTableType) {
        // Build table according to constraint and key.
        if (bTableType.constraint == null) {
            setState(withReadOnly(bTableType, "table"));
            return;
        }

        String constraintRepr = transform(bTableType.constraint);
        String keyRepr = null;
        if (bTableType.fieldNameList != null) {
            keyRepr = String.join(", ", bTableType.fieldNameList);
        } else if (bTableType.keyTypeConstraint != null) {
            keyRepr = transform(bTableType.keyTypeConstraint);
        }

        String stringRepr = (keyRepr != null)
                ? String.format("table<%s> key(%s)", constraintRepr, keyRepr)
                : String.format("table<%s>", constraintRepr);
        setState(withReadOnly(bTableType, stringRepr));
    }

    @Override
    public void visit(BErrorType bErrorType) {
        if (bErrorType.tsymbol != null && bErrorType.tsymbol.name != null
                && !bErrorType.tsymbol.name.value.startsWith("$")) {
            setState(getImportedName(bErrorType, String.valueOf(bErrorType.tsymbol)));
        } else {
            setState(String.format("error<%s>", transform(bErrorType.detailType)));
        }
    }

    @Override
    public void visit(BStreamType bStreamType) {
        String stringRepr = getImportedName(bStreamType, bStreamType.getKind().typeName());
        if (bStreamType.constraint.tag != TypeTags.ANY) {
            String constraintRepr = transform(bStreamType.constraint);
            if (bStreamType.error != null) {
                String errorRepr = transform(bStreamType.error);
                stringRepr = String.format("%s<%s,%s>", stringRepr, constraintRepr, errorRepr);
            } else {
                stringRepr = String.format("%s<%s>", stringRepr, constraintRepr);
            }
        }
        setState(stringRepr);
    }

    @Override
    public void visit(BInvokableType bInvokableType) {
        String retTypeWithParam = transform(bInvokableType.retType);
        if (bInvokableType.retType.getKind() != TypeKind.NIL) {
            retTypeWithParam = "(" + retTypeWithParam + ")";
        }

        String restParam = "";
        if (bInvokableType.restType instanceof BArrayType) {
            BType restEType = ((BArrayType) bInvokableType.restType).eType;
            String restETypeRepr = transform(restEType);
            if (!bInvokableType.paramTypes.isEmpty()) {
                restParam = restParam + ", ";
            }
            restParam = restParam + restETypeRepr + "...";
        }

        String typeSignatureRepr = "";
        if (!bInvokableType.paramTypes.isEmpty()) {
            typeSignatureRepr = bInvokableType.paramTypes
                    .stream().map(this::transform)
                    .collect(Collectors.joining(","));
        }
        typeSignatureRepr = String.format("(%s%s) returns %s",
                typeSignatureRepr, restParam, retTypeWithParam);

        String stringRepr = Symbols.isFlagOn(bInvokableType.flags, Flags.ISOLATED)
                ? "isolated function " + typeSignatureRepr
                : "function " + typeSignatureRepr;
        setState(stringRepr);
    }

    @Override
    public void visit(BTypedescType bTypedescType) {
        String stringRepr = getImportedName(bTypedescType, bTypedescType.getKind().typeName());
        if (bTypedescType.constraint.tag != TypeTags.ANY) {
            String constraintRepr = transform(bTypedescType.constraint);
            stringRepr = String.format("%s<%s>", stringRepr, constraintRepr);
        }
        setState(stringRepr);
    }

    @Override
    public void visit(BTupleType bTupleType) {
        String stringRepr = bTupleType.tupleTypes
                .stream().map(this::transform)
                .collect(Collectors.joining(","));
        if (bTupleType.restType != null) {
            String restRepr = transform(bTupleType.restType);
            if (bTupleType.tupleTypes.isEmpty()) {
                stringRepr = String.format("[%s]", restRepr);
            } else {
                stringRepr = String.format("[%s,%s...]", stringRepr, restRepr);
            }
        } else {
            stringRepr = String.format("[%s]", stringRepr);
        }
        setState(withReadOnly(bTupleType, stringRepr));
    }

    @Override
    public void visit(BIntersectionType bIntersectionType) {
        StringJoiner joiner = new StringJoiner(" & ", "(", ")");
        bIntersectionType.getConstituentTypes().stream()
                .map(type -> type.tag == TypeTags.NIL ? "()" : transform(type))
                .forEach(joiner::add);
        setState(joiner.toString());
    }

    @Override
    public void visit(BXMLType bxmlType) {
        String stringRepr;
        if (bxmlType.constraint != null
                && !(bxmlType.constraint.tag == TypeTags.UNION
                && bxmlType.constraint instanceof BUnionType
                && ((BUnionType) bxmlType.constraint).getMemberTypes().size() == 4)
        ) {
            stringRepr = "xml<" + transform(bxmlType.constraint) + ">";
        } else {
            stringRepr = "xml";
        }
        setState(withReadOnly(bxmlType, stringRepr));
    }

    @Override
    public void visit(BFutureType bFutureType) {
        String stringRepr = bFutureType.getKind().typeName();
        if (bFutureType.constraint.tag != TypeTags.NONE
                && bFutureType.constraint.tag != TypeTags.SEMANTIC_ERROR
                && bFutureType.constraint.tag != TypeTags.NIL) {
            stringRepr = stringRepr + "<" + transform(bFutureType.constraint) + ">";
        }
        setState(stringRepr);
    }

    @Override
    public void visit(BRecordType bRecordType) {
        // TODO: Should implement stringify logic?
        visit((BType) bRecordType);
    }

    @Override
    public void visit(BObjectType bObjectType) {
        // TODO: Should implement stringify logic?
        visit((BType) bObjectType);
    }

    @Override
    public void visit(BStructureType bStructureType) {
        visit((BType) bStructureType);
    }

    @Override
    public void visit(BAnnotationType bAnnotationType) {
        visit((BType) bAnnotationType);
    }

    @Override
    public void visit(BBuiltInRefType bBuiltInRefType) {
        visit((BType) bBuiltInRefType);
    }

    @Override
    public void visit(BAnyType bAnyType) {
        visit((BType) bAnyType);
    }

    @Override
    public void visit(BAnydataType bAnydataType) {
        visit((BType) bAnydataType);
    }

    @Override
    public void visit(BFiniteType bFiniteType) {
        visit((BType) bFiniteType);
    }

    @Override
    public void visit(BJSONType bjsonType) {
        visit((BType) bjsonType);
    }

    @Override
    public void visit(BParameterizedType bParameterizedType) {
        visit((BType) bParameterizedType);
    }

    @Override
    public void visit(BNeverType bNeverType) {
        visit((BType) bNeverType);
    }

    @Override
    public void visit(BNilType bNilType) {
        visit((BType) bNilType);
    }

    @Override
    public void visit(BNoType bNoType) {
        visit((BType) bNoType);
    }

    @Override
    public void visit(BPackageType bPackageType) {
        visit((BType) bPackageType);
    }

    @Override
    public void visit(BServiceType bServiceType) {
        visit((BType) bServiceType);
    }

    @Override
    public void visit(BHandleType bHandleType) {
        visit((BType) bHandleType);
    }

    private String withReadOnly(BType bType, String withoutSuffix) {
        if (Symbols.isFlagOn(bType.flags, Flags.READONLY)) {
            return withoutSuffix + " & readonly";
        }
        return withoutSuffix;
    }

    private String getImportedName(BType bType, String defaultName) {
        try {
            Matcher importTypeMatcher = IMP_WITH_VER_PATTERN.matcher(bType.toString());
            if (importTypeMatcher.matches()) {
                // Then we need to infer the type and find the imports
                // that are required for the inferred type.
                String orgName = importTypeMatcher.group(1);
                String[] compNames = importTypeMatcher.group(2).split("\\.");
                String impType = quotedIdentifier(importTypeMatcher.group(3));

                // Import prefix is almost always the final comp name.
                String importPrefix = importCreator.createImport(orgName, compNames);
                foundImports.add(importPrefix);
                return importPrefix + QUALIFIED_NAME_SEP + impType;
            }
            Matcher defMatcher = IMP_WITHOUT_VER_PATTERN.matcher(bType.toString());
            if (defMatcher.matches()) {
                String prefix = quotedIdentifier(defMatcher.group(1));
                String type = defMatcher.group(2);
                return prefix + QUALIFIED_NAME_SEP + type;
            }
            return defaultName;
        } catch (InvokerException e) {
            // Should not fail.
            throw new IllegalStateException(e);
        }
    }

    private String quotedIdentifier(String identifier) {
        if (identifier.startsWith(QUOTE)) {
            return identifier;
        }
        return QUOTE + identifier;
    }

    @Override
    public void resetState() {
        setState("");
    }

    /**
     * Creates an import and fetches a possible import prefix.
     */
    public interface ImportCreator {
        /**
         * Get a possible unused identifier to import the package given.
         *
         * @param orgName   Org name of the import.
         * @param compNames Name parts of imported. (to be dot separated)
         * @return The import prefix and the type for the import.
         * @throws InvokerException If import resolution failed.
         */
        String createImport(String orgName, String... compNames) throws InvokerException;
    }
}
