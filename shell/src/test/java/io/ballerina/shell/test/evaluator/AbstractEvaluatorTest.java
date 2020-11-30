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
import io.ballerina.shell.exceptions.BallerinaShellException;
import io.ballerina.shell.invoker.Invoker;
import io.ballerina.shell.invoker.classload.ClassLoadInvoker;
import io.ballerina.shell.invoker.classload.NoExitVmSecManager;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;
import io.ballerina.shell.test.TestUtils;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Base class for evaluator tests.
 */
public abstract class AbstractEvaluatorTest {
    private static class TestCaseLine {
        String code;
        String stdout = "";
        String expr;
        int exitCode = 0;
    }

    private static class TestCase extends ArrayList<TestCaseLine> {
    }

    private static class TestInvoker extends ClassLoadInvoker {
        private String output;
        private int expectingExitCode;

        @Override
        protected int invokeMethod(Method method) throws IllegalAccessException {
            String[] args = new String[0];

            PrintStream stdErr = System.err;
            PrintStream stdOut = System.out;
            ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
            NoExitVmSecManager secManager = new NoExitVmSecManager(System.getSecurityManager());
            try {
                System.setErr(new PrintStream(new ByteArrayOutputStream()));
                System.setOut(new PrintStream(stdOutBaOs));
                System.setSecurityManager(secManager);
                return (int) method.invoke(null, new Object[]{args});
            } catch (InvocationTargetException ignored) {
                Assert.assertEquals(secManager.getExitCode(), expectingExitCode, "Exit code was unexpected");
                return 0;
            } finally {
                // Restore everything
                this.output = new String(stdOutBaOs.toByteArray());
                System.setSecurityManager(null);
                System.setErr(stdErr);
                System.setOut(stdOut);
            }
        }
    }

    protected void testEvaluate(String fileName) throws BallerinaShellException {
        TestInvoker invoker = getInvoker();
        Evaluator evaluator = getEvaluator(invoker);
        TestCase testCase = TestUtils.loadTestCases(fileName, TestCase.class);
        for (TestCaseLine testCaseLine : testCase) {
            invoker.expectingExitCode = testCaseLine.exitCode;
            String expr = evaluator.evaluate(testCaseLine.code);
            Assert.assertEquals(invoker.output, testCaseLine.stdout);
            Assert.assertEquals(expr, testCaseLine.expr);
        }
    }

    private TestInvoker getInvoker() {
        return new TestInvoker();
    }

    private Evaluator getEvaluator(Invoker invoker) {
        Evaluator evaluator = new Evaluator();
        evaluator.setSnippetFactory(new BasicSnippetFactory());
        evaluator.setTreeParser(new TrialTreeParser());
        evaluator.setPreprocessor(new SeparatorPreprocessor());
        evaluator.setInvoker(invoker);
        return evaluator;
    }
}
