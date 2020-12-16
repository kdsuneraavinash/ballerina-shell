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

import org.ballerinalang.model.elements.PackageID;
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
import org.wso2.ballerinalang.compiler.util.Names;

import java.util.ArrayList;
import java.util.List;

/**
 * Visits all the nodes and informs whether the type needs
 * elevating due to some types not being visible to the REPL.
 */
public class BTypeElevatorVisitor extends AbstractTypeVisitor {
    private final List<BType> invisibleTypes;
    private ElevatedType elevatedType;

    public BTypeElevatorVisitor() {
        this.elevatedType = ElevatedType.NONE;
        this.invisibleTypes = new ArrayList<>();
    }

    public ElevatedType getElevatedType() {
        return elevatedType;
    }

    public List<BType> getInvisibleTypes() {
        return invisibleTypes;
    }

    public boolean isVisible() {
        return this.invisibleTypes.isEmpty();
    }

    @Override
    public void visit(BAnnotationType bAnnotationType) {
        setVisibility(bAnnotationType);
    }

    @Override
    public void visit(BArrayType bArrayType) {
        super.visit(bArrayType);
        setVisibility(bArrayType);
    }

    @Override
    public void visit(BBuiltInRefType bBuiltInRefType) {
        setVisibility(bBuiltInRefType);
    }

    @Override
    public void visit(BAnyType bAnyType) {
        setVisibility(bAnyType);
    }

    @Override
    public void visit(BAnydataType bAnydataType) {
        setVisibility(bAnydataType);
    }

    @Override
    public void visit(BErrorType bErrorType) {
        super.visit(bErrorType);
        setVisibility(bErrorType);
    }

    @Override
    public void visit(BFiniteType bFiniteType) {
        setVisibility(bFiniteType);
    }

    @Override
    public void visit(BInvokableType bInvokableType) {
        super.visit(bInvokableType);
        setVisibility(bInvokableType);
    }

    @Override
    public void visit(BJSONType bjsonType) {
        setVisibility(bjsonType);
    }

    @Override
    public void visit(BMapType bMapType) {
        super.visit(bMapType);
        setVisibility(bMapType);
    }

    @Override
    public void visit(BStreamType bStreamType) {
        super.visit(bStreamType);
        setVisibility(bStreamType);
    }

    @Override
    public void visit(BTypedescType bTypedescType) {
        super.visit(bTypedescType);
        setVisibility(bTypedescType);
    }

    @Override
    public void visit(BParameterizedType bParameterizedType) {
        setVisibility(bParameterizedType);
    }

    @Override
    public void visit(BNeverType bNeverType) {
        setVisibility(bNeverType);
    }

    @Override
    public void visit(BNilType bNilType) {
        setVisibility(bNilType);
    }

    @Override
    public void visit(BNoType bNoType) {
        setVisibility(bNoType);
    }

    @Override
    public void visit(BPackageType bPackageType) {
        setVisibility(bPackageType);
    }

    @Override
    public void visit(BServiceType bServiceType) {
        setVisibility(bServiceType);
    }

    @Override
    public void visit(BStructureType bStructureType) {
        super.visit(bStructureType);
        setVisibility(bStructureType);
    }

    @Override
    public void visit(BTupleType bTupleType) {
        super.visit(bTupleType);
        setVisibility(bTupleType);
    }

    @Override
    public void visit(BUnionType bUnionType) {
        super.visit(bUnionType);
        setVisibility(bUnionType);
    }

    @Override
    public void visit(BIntersectionType bIntersectionType) {
        super.visit(bIntersectionType);
        setVisibility(bIntersectionType);
    }

    @Override
    public void visit(BXMLType bxmlType) {
        super.visit(bxmlType);
        setVisibility(bxmlType);
    }

    @Override
    public void visit(BTableType bTableType) {
        super.visit(bTableType);
        setVisibility(bTableType);
    }

    @Override
    public void visit(BRecordType bRecordType) {
        super.visit(bRecordType);
        setVisibility(bRecordType);
    }

    @Override
    public void visit(BObjectType bObjectType) {
        super.visit(bObjectType);
        setVisibility(bObjectType);
    }

    @Override
    public void visit(BType bType) {
        setVisibility(bType);
    }

    @Override
    public void visit(BFutureType bFutureType) {
        super.visit(bFutureType);
        setVisibility(bFutureType);
    }

    @Override
    public void visit(BHandleType bHandleType) {
        setVisibility(bHandleType);
    }

    /**
     * Sets the visibility depending on the type given.
     * This will be called on all the visited nodes,
     * This would elevate the type if a visited type is not visible.
     * If all of the visited nodes are error types, elevates to 'error'.
     * If some of the visited nodes are error types, elevates to 'aby|error'.
     * If none of the visited nodes are error types, elevates to 'any'.
     *
     * @param type Typ[e to visit.
     */
    private void setVisibility(BType type) {
        if (type == null || type.tsymbol == null || type.tsymbol.pkgID == null) {
            return;
        }

        // Change the type according to the visibility.
        if (elevatedType != ElevatedType.ANY_ERROR) {
            if (type.getKind() == TypeKind.ERROR) {
                elevatedType = elevatedType == ElevatedType.ANY
                        ? ElevatedType.ANY_ERROR : ElevatedType.ERROR;
            } else {
                elevatedType = elevatedType == ElevatedType.ERROR
                        ? ElevatedType.ANY_ERROR : ElevatedType.ANY;
            }
        }
        if (type.tsymbol.pkgID == PackageID.DEFAULT
                || type.tsymbol.pkgID.equals(PackageID.ANNOTATIONS)
                || type.tsymbol.pkgID.name == Names.DEFAULT_PACKAGE
                || Symbols.isPublic(type.tsymbol)) {
            // In this module so visible - dont change value
            return;
        }
        this.invisibleTypes.add(type);
    }

    /**
     * Type after elevating the type of the visited node.
     * If not required, the type is fully visible.
     * Otherwise it would be one of any, error, any|error
     */
    public enum ElevatedType {
        NONE("()"),
        ANY("any"),
        ERROR("error"),
        ANY_ERROR("(any|error)");

        private final String repr;

        ElevatedType(String repr) {
            this.repr = repr;
        }

        @Override
        public String toString() {
            return repr;
        }
    }
}
