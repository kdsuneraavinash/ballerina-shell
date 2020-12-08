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
public class FlowControlEvaluatorTest extends AbstractEvaluatorTest {
    private static final String ELVIS_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.flow.elvis.json";
    private static final String IF_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.flow.if.json";
    private static final String MATCH_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.flow.match.json";
    private static final String WHILE_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.flow.while.json";
    private static final String FOREACH_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.flow.foreach.json";

    @Test
    public void testEvaluateElvis() throws BallerinaShellException {
        testEvaluate(ELVIS_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateIf() throws BallerinaShellException {
        testEvaluate(IF_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateMatch() throws BallerinaShellException {
        testEvaluate(MATCH_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateWhile() throws BallerinaShellException {
        testEvaluate(WHILE_EVALUATOR_TESTCASE);
    }

    @Test
    public void testEvaluateForEach() throws BallerinaShellException {
        testEvaluate(FOREACH_EVALUATOR_TESTCASE);
    }
}
