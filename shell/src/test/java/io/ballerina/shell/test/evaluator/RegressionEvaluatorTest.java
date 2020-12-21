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

package io.ballerina.shell.test.evaluator;

import io.ballerina.shell.exceptions.BallerinaShellException;
import org.testng.annotations.Test;

/**
 * Test simple snippets.
 */
public class RegressionEvaluatorTest extends AbstractEvaluatorTest {
    private static final String PREV_IMPORT_EVALUATOR_TESTCASE = "testcases/evaluator/regression.external.prev.json";
    private static final String FUNCTION_NAME_EVALUATOR_TESTCASE = "testcases/evaluator/regression.function.name.json";

    @Test
    public void testEvaluatePrevImport() throws BallerinaShellException {
        // Functions/var dclns with imports should cause the import in all later snippets.
        // TODO: Add math lib
        // testEvaluate(PREV_IMPORT_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateFunctionName() throws BallerinaShellException {
        // If a type is defined, the name cannot be used by a variable and vice versa.
        testEvaluate(FUNCTION_NAME_EVALUATOR_TESTCASE);
    }
}
