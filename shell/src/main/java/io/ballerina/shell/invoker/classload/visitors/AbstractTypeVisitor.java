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

import org.wso2.ballerinalang.compiler.semantics.model.TypeVisitor;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnnotationType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnyType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BAnydataType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BBuiltInRefType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visits all the types in a given type.
 * Will visit nested/parameterized/... types and perform given operations.
 * Will not visit the same node twice. Uses a vsited set to do so.
 */
public abstract class AbstractTypeVisitor implements TypeVisitor {
    private final Set<BType> visitedTypes;

    protected AbstractTypeVisitor() {
        visitedTypes = new HashSet<>();
    }

    @Override
    public void visit(BArrayType bArrayType) {
        accept(bArrayType.eType);
    }

    @Override
    public void visit(BErrorType bErrorType) {
        accept(bErrorType.detailType);
    }

    @Override
    public void visit(BInvokableType bInvokableType) {
        accept(bInvokableType.retType);
        accept(bInvokableType.paramTypes);
        if (bInvokableType.restType != null) {
            accept(bInvokableType.restType);
        }
    }

    @Override
    public void visit(BMapType bMapType) {
        accept(bMapType.constraint);
    }

    @Override
    public void visit(BStreamType bStreamType) {
        accept(bStreamType.constraint);
        if (bStreamType.error != null) {
            accept(bStreamType.error);
        }
    }

    @Override
    public void visit(BTypedescType bTypedescType) {
        accept(bTypedescType.constraint);
    }

    @Override
    public void visit(BStructureType bStructureType) {
        accept(bStructureType.fields);
    }

    @Override
    public void visit(BTupleType bTupleType) {
        accept(bTupleType.tupleTypes);
    }

    @Override
    public void visit(BUnionType bUnionType) {
        accept(bUnionType.getMemberTypes());
    }

    @Override
    public void visit(BIntersectionType bIntersectionType) {
        accept(bIntersectionType.getConstituentTypes());
    }

    @Override
    public void visit(BXMLType bxmlType) {
        if (bxmlType.constraint != null) {
            accept(bxmlType.constraint);
        }
    }

    @Override
    public void visit(BTableType bTableType) {
        if (bTableType.constraint != null) {
            accept(bTableType.constraint);
        }
        if (bTableType.keyTypeConstraint != null) {
            accept(bTableType.keyTypeConstraint);
        }
    }

    @Override
    public abstract void visit(BAnnotationType bAnnotationType);

    @Override
    public void visit(BRecordType bRecordType) {
        accept(bRecordType.fields);
    }

    @Override
    public void visit(BObjectType bObjectType) {
        accept(bObjectType.fields);
    }

    @Override
    public void visit(BFutureType bFutureType) {
        accept(bFutureType.constraint);
    }

    @Override
    public abstract void visit(BBuiltInRefType bBuiltInRefType);

    @Override
    public abstract void visit(BAnyType bAnyType);

    @Override
    public abstract void visit(BAnydataType bAnydataType);

    @Override
    public abstract void visit(BFiniteType bFiniteType);

    @Override
    public abstract void visit(BJSONType bjsonType);

    @Override
    public abstract void visit(BParameterizedType bParameterizedType);

    @Override
    public abstract void visit(BNeverType bNeverType);

    @Override
    public abstract void visit(BNilType bNilType);

    @Override
    public abstract void visit(BNoType bNoType);

    @Override
    public abstract void visit(BPackageType bPackageType);

    @Override
    public abstract void visit(BServiceType bServiceType);

    @Override
    public abstract void visit(BHandleType bHandleType);

    @Override
    public abstract void visit(BType bType);

    protected void accept(Map<String, BField> fields) {
        fields.values().stream().map(BField::getType)
                .forEach(this::accept);
    }

    protected void accept(Collection<BType> types) {
        types.forEach(this::accept);
    }

    /**
     * Accepts a node and calls correct visit method on it.
     * All the visits must happen via this method.
     *
     * @param type Type to visit.
     */
    public void accept(BType type) {
        if (!visitedTypes.contains(type)) {
            visitedTypes.add(type);
            Objects.requireNonNull(type, "Null type cannot be visited");
            type.accept(this);
        }
    }
}
