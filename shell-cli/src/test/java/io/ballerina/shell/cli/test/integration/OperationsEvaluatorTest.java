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

package io.ballerina.shell.cli.test.integration;

import org.testng.annotations.Test;

/**
 * Test simple snippets.
 */
public class OperationsEvaluatorTest extends AbstractIntegrationTest {
    private static final String SHIFT_OPERATION_TESTCASE = "testcases/operations.shift.json";
    private static final String COMPOUND_OPERATION_TESTCASE = "testcases/operations.comp.json";
    private static final String EQUALITY_OPERATION_TESTCASE = "testcases/operations.equality.json";
    private static final String CAST_OPERATION_TESTCASE = "testcases/operations.cast.json";
    private static final String IMMUTABLE_OPERATION_TESTCASE = "testcases/operations.immutable.json";

    @Test
    public void testEvaluateShift() throws Exception {
        test(SHIFT_OPERATION_TESTCASE);
    }

    @Test
    public void testEvaluateCompound() throws Exception {
        test(COMPOUND_OPERATION_TESTCASE);
    }

    @Test
    public void testEvaluateEquality() throws Exception {
        test(EQUALITY_OPERATION_TESTCASE);
    }

    @Test
    public void testEvaluateCast() throws Exception {
        test(CAST_OPERATION_TESTCASE);
    }

    @Test
    public void testEvaluateImmutable() throws Exception {
        test(IMMUTABLE_OPERATION_TESTCASE);
    }
}
