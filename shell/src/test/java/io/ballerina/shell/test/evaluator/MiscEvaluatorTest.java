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
public class MiscEvaluatorTest extends AbstractEvaluatorTest {
    private static final String ENUM_EVALUATOR_TESTCASE = "testcases/evaluator/evaluator.enum.json";

    @Test
    public void testEvaluateEnum() throws BallerinaShellException {
        // TODO: An enum can also be used as a type.
        //  Language language = getLanguage("e"); language
        //  Language sinhala = "Sinhala"; sinhala
        //  EN english = "English"; english
        //  Language type is not correctly inferred.
        //  The enums are extracted so the type inferred is incorrect.
        testEvaluate(ENUM_EVALUATOR_TESTCASE);
    }
}
