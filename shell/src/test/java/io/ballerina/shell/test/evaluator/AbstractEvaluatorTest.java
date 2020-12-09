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

import io.ballerina.shell.DiagnosticKind;
import io.ballerina.shell.Evaluator;
import io.ballerina.shell.EvaluatorBuilder;
import io.ballerina.shell.exceptions.BallerinaShellException;
import io.ballerina.shell.invoker.classload.ClassLoadInvoker;
import io.ballerina.shell.test.TestUtils;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Base class for evaluator tests.
 */
public abstract class AbstractEvaluatorTest {
    private static class TestCaseLine {
        String description;
        String code;
        String expr;
        String stdout = "";
        int exitCode = 0;
    }

    private static class TestCase extends ArrayList<TestCaseLine> {
    }

    private static class TestInvoker extends ClassLoadInvoker {
        private String output;
        private TestCaseLine testCaseLine;

        @Override
        protected int invokeMethod(Method method) throws IllegalAccessException {
            PrintStream stdOut = System.out;
            ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
            try {
                System.setOut(new PrintStream(stdOutBaOs, true, Charset.defaultCharset()));
                int exitCode = super.invokeMethod(method);
                Assert.assertEquals(exitCode, testCaseLine.exitCode, testCaseLine.description);
                return exitCode;
            } finally {
                this.output = stdOutBaOs.toString(Charset.defaultCharset());
                this.output = this.output.replace("\r\n", "\n");
                System.setOut(stdOut);
            }
        }
    }

    protected void testEvaluate(String fileName) throws BallerinaShellException {
        TestInvoker invoker = getInvoker();
        Evaluator evaluator = new EvaluatorBuilder()
                .treeParser(TestUtils.getTestTreeParser())
                .invoker(invoker).build();
        TestCase testCase = TestUtils.loadTestCases(fileName, TestCase.class);
        for (TestCaseLine testCaseLine : testCase) {
            try {
                invoker.testCaseLine = testCaseLine;
                String expr = evaluator.evaluate(testCaseLine.code);
                Assert.assertEquals(invoker.output, testCaseLine.stdout, testCaseLine.description);
                Assert.assertEquals(expr, testCaseLine.expr, testCaseLine.description);
            } catch (BallerinaShellException e) {
                StringBuilder diagnosticsStr = new StringBuilder();
                evaluator.diagnostics().stream()
                        .filter(d -> d.getKind() == DiagnosticKind.ERROR)
                        .map(s -> s + "\n")
                        .forEach(diagnosticsStr::append);
                Assert.fail(
                        "Exception occurred in:" + bracketed(testCaseLine.description) +
                                "err:" + bracketed(e.getMessage()) +
                                "diagnostics:" + bracketed(diagnosticsStr));
                throw e;
            } finally {
                evaluator.resetDiagnostics();
            }
        }
    }

    protected static String bracketed(Object input) {
        return " [" + input + "] ";
    }

    private TestInvoker getInvoker() {
        return new TestInvoker();
    }
}
