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
    private static final String FUNCTION_NAME_EVALUATOR_TESTCASE = "testcases/evaluator/regression.function.name.json";
    private static final String SAME_IMPORT_EVALUATOR_TESTCASE = "testcases/evaluator/regression.same.import.json";

    @Test
    public void testEvaluateFunctionName() throws BallerinaShellException {
        // If a type is defined, the name cannot be used by a variable and vice versa.
        testEvaluate(FUNCTION_NAME_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateSameImport() throws BallerinaShellException {
        // Import can be again and again imported with same prefix.
        testEvaluate(SAME_IMPORT_EVALUATOR_TESTCASE);
    }
}
