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

import io.ballerina.shell.Evaluator;
import io.ballerina.shell.EvaluatorBuilder;
import io.ballerina.shell.exceptions.BallerinaShellException;
import io.ballerina.shell.test.TestUtils;
import io.ballerina.shell.test.evaluator.base.TestCase;
import io.ballerina.shell.test.evaluator.base.TestInvoker;
import io.ballerina.shell.test.evaluator.base.TestSession;
import org.testng.Assert;

/**
 * Base class for evaluator tests.
 * TODO: Find a way to test concurrency.
 * TODO: Add db lib/http lib support and test Transactions
 */
public abstract class AbstractEvaluatorTest {
    /**
     * Tests a json file containing test case session.
     * This is a E2E test of the evaluator.
     *
     * @param fileName File containing test cases.
     */
    protected void testEvaluate(String fileName) throws BallerinaShellException {
        // Create evaluator
        TestInvoker invoker = new TestInvoker();
        Evaluator evaluator = new EvaluatorBuilder()
                .treeParser(TestUtils.getTestTreeParser())
                .invoker(invoker).build();
        evaluator.initialize();

        TestSession testSession = TestUtils.loadTestCases(fileName, TestSession.class);
        for (TestCase testCase : testSession) {
            try {
                invoker.setTestCaseStatement(testCase);
                String expr = evaluator.evaluate(testCase.getCode());
                Assert.assertEquals(invoker.getOutput(), testCase.getStdout(), testCase.getDescription());
                Assert.assertEquals(expr, testCase.getExpr(), testCase.getDescription());
                Assert.assertNull(testCase.getError(), testCase.getDescription());
            } catch (BallerinaShellException e) {
                if (testCase.getError() != null) {
                    Assert.assertEquals(testCase.getError(), e.getClass().getSimpleName());
                    continue;
                }
                Assert.fail(String.format("Exception occurred in: %s, error: %s, with diagnostics: %s",
                        testCase.getDescription(), e.getMessage(), evaluator.diagnostics()));
            } finally {
                evaluator.resetDiagnostics();
            }
        }
    }
}
