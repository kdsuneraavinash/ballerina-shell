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

import java.security.Permission;

/**
 * A helper class for class load invoker so that
 * the system.exit calls will not exit the VM.
 * TODO: Remove this and fix VM exit bug.
 */
public class NoExitVmSecManager extends SecurityManager {
    private final SecurityManager baseSecurityManager;

    public NoExitVmSecManager(SecurityManager baseSecurityManager) {
        this.baseSecurityManager = baseSecurityManager;
    }

    @Override
    public void checkPermission(Permission permission) {
        if (permission.getName().startsWith("exitVM")) {
            throw new SecurityException("System exit not allowed");
        }
        if (baseSecurityManager != null) {
            baseSecurityManager.checkPermission(permission);
        }
    }
}
