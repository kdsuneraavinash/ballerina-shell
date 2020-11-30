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

package io.ballerina.shell.test.unit;

import io.ballerina.shell.exceptions.PreprocessorException;
import io.ballerina.shell.preprocessor.Preprocessor;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.test.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test preprocessor use cases.
 */
public class SeparatorPreprocessorTest {
    private static final String TESTCASES = "testcases/preprocessor.separator.json";

    private static class TestCase {
        String name;
        String input;
        List<String> expected;
        boolean isException = false;
    }

    private static class TestCases extends ArrayList<TestCase> {
    }

    @Test
    public void testProcess() {
        List<TestCase> testCases = TestUtils.loadTestCases(TESTCASES, TestCases.class);
        Preprocessor preprocessor = new SeparatorPreprocessor();
        for (TestCase testCase : testCases) {
            try {
                Collection<String> actual = preprocessor.process(testCase.input);
                Assert.assertEquals(actual, testCase.expected, testCase.name);
                Assert.assertFalse(testCase.isException, testCase.name);
            } catch (PreprocessorException e) {
                Assert.assertTrue(testCase.isException, testCase.name);
            }
        }
    }
}
