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
import org.wso2.ballerinalang.util.Flags;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a type into its string format.
 * TODO: Improve this to pass all test cases.
 */
public class BTypeStringGen extends AbstractTypeVisitor {
    protected static final Pattern IMPORT_TYPE_PATTERN = Pattern.compile("(.*)/(.*):[0-9.]*:(.*)");
    protected static final String QUALIFIED_NAME_SEP = ":";
    private static final String QUOTE = "'";
    private final Set<String> foundImports;
    private final ImportCreator importCreator;
    private String stringRepr;

    public BTypeStringGen(Set<String> foundImports, ImportCreator importCreator) {
        this.foundImports = foundImports;
        this.importCreator = importCreator;
        this.stringRepr = "";
    }

    @Override
    public void visit(BType bType) {
        try {
            Matcher importTypeMatcher = IMPORT_TYPE_PATTERN.matcher(bType.toString());
            if (importTypeMatcher.matches()) {
                // Then we need to infer the type and find the imports
                // that are required for the inferred type.
                String orgName = importTypeMatcher.group(1);
                String[] compNames = importTypeMatcher.group(2).split("\\.");
                String impType = importTypeMatcher.group(3);
                if (!impType.startsWith(QUOTE)) {
                    impType = QUOTE + impType;
                }

                // Import prefix is almost always the final comp name.
                String importPrefix = importCreator.createImport(orgName, compNames);
                String varType = importPrefix + QUALIFIED_NAME_SEP + impType;
                this.foundImports.add(importPrefix);
                this.stringRepr = varType;
            } else {
                this.stringRepr = bType.toString();
            }
        } catch (InvokerException e) {
            // Should not fail.
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void visit(BMapType bMapType) {
        // String representation is map<CONSTRAINT>.
        // Add readonly flag if necessary.
        bMapType.constraint.accept(this);
        String stringRepr = getStringRepr();
        stringRepr = "map<" + stringRepr + ">";
        this.stringRepr = withReadOnly(bMapType, stringRepr);
    }

    @Override
    public void visit(BUnionType bUnionType) {
        // Simply add all sub types separated by |.
        // Add a ? at the end if nullable.
        // Also, if the type needs to be elevated, (because exported type not visible),
        // do so.
        Set<String> typeStrings = new HashSet<>();
        for (BType memberType : bUnionType.getMemberTypes()) {
            memberType.accept(this);
            typeStrings.add(this.getStringRepr());
        }
        String stringRepr = "(" + String.join("|", typeStrings) + ")";
        if (bUnionType.isNullable()) {
            stringRepr = stringRepr + "?";
        }
        this.stringRepr = stringRepr;
    }

    @Override
    public void visit(BArrayType bArrayType) {
        // BArrayType string is always in format 'eType[.....]'
        // And there are no types between [].
        // So we can convert to string, and replace first 'eType' part.
        // If the eType needs to be elevated, return it.
        String original = bArrayType.toString();
        String originalETypeStr = bArrayType.eType.toString();
        bArrayType.eType.accept(this);
        String eTypeStr = getStringRepr();
        this.stringRepr = eTypeStr + original.substring(originalETypeStr.length());
    }

    @Override
    public void visit(BTableType bTableType) {
        // Build table according to constraint and key.
        if (bTableType.constraint == null) {
            this.stringRepr = withReadOnly(bTableType, "table");
            return;
        }

        bTableType.constraint.accept(this);
        String constraintRepr = getStringRepr();

        String keyRepr = null;
        if (bTableType.fieldNameList != null) {
            keyRepr = String.join(", ", bTableType.fieldNameList);
        } else if (bTableType.keyTypeConstraint != null) {
            bTableType.keyTypeConstraint.accept(this);
            keyRepr = getStringRepr();
        }

        String stringRepr = (keyRepr != null)
                ? String.format("table<%s> key(%s)", constraintRepr, keyRepr)
                : String.format("table<%s>", constraintRepr);
        this.stringRepr = withReadOnly(bTableType, stringRepr);
    }

    @Override
    public void visit(BErrorType bErrorType) {
        // TODO: Implement logic
        visit((BType) bErrorType);
    }

    @Override
    public void visit(BStreamType bStreamType) {
        // TODO: Implement logic
        visit((BType) bStreamType);
    }

    @Override
    public void visit(BTypedescType bTypedescType) {
        // TODO: Implement logic
        visit((BType) bTypedescType);
    }

    @Override
    public void visit(BTupleType bTupleType) {
        // TODO: Implement logic
        visit((BType) bTupleType);
    }

    @Override
    public void visit(BIntersectionType bIntersectionType) {
        // TODO: Implement logic
        visit((BType) bIntersectionType);
    }

    @Override
    public void visit(BXMLType bxmlType) {
        // TODO: Implement logic
        visit((BType) bxmlType);
    }

    @Override
    public void visit(BRecordType bRecordType) {
        // TODO: Implement logic
        visit((BType) bRecordType);
    }

    @Override
    public void visit(BObjectType bObjectType) {
        // TODO: Implement logic
        visit((BType) bObjectType);
    }

    @Override
    public void visit(BFutureType bFutureType) {
        // TODO: Implement logic
        visit((BType) bFutureType);
    }

    @Override
    public void visit(BStructureType bStructureType) {
        // TODO: Implement logic
        visit((BType) bStructureType);
    }

    @Override
    public void visit(BInvokableType bInvokableType) {
        // TODO: Implement logic
        visit((BType) bInvokableType);
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

    public String getStringRepr() {
        return stringRepr;
    }

    private String withReadOnly(BType bType, String withoutSuffix) {
        if (Symbols.isFlagOn(bType.flags, Flags.READONLY)) {
            return withoutSuffix + " & readonly";
        }
        return withoutSuffix;
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
