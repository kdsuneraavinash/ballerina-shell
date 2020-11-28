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

package io.ballerina.shell.evaluator;

import io.ballerina.shell.Evaluator;
import io.ballerina.shell.TestUtils;
import io.ballerina.shell.exceptions.BallerinaShellException;
import io.ballerina.shell.invoker.Invoker;
import io.ballerina.shell.invoker.classload.ClassLoadInvoker;
import io.ballerina.shell.invoker.classload.NoExitVmSecManager;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractEvaluatorTest {
    private static final Path BALLERINA_HOME_PATH = Paths.get("../home");

    private static class TestCaseLine {
        String code;
        String stdout = "";
        ArrayList<String> exprs = new ArrayList<>();
        int exitCode = 0;
    }

    private static class TestCase extends ArrayList<TestCaseLine> {
    }

    private static class TestInvoker extends ClassLoadInvoker {
        private String output;
        private int expectingExitCode;

        public TestInvoker(Path ballerinaHome) {
            super(ballerinaHome);
        }

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
            List<String> output = new ArrayList<>();
            evaluator.evaluate(testCaseLine.code)
                    .stream().filter(Objects::nonNull)
                    .map(String::valueOf).forEach(output::add);
            Assert.assertEquals(invoker.output, testCaseLine.stdout);
            Assert.assertEquals(output, testCaseLine.exprs);
        }
    }

    private TestInvoker getInvoker() {
        return new TestInvoker(BALLERINA_HOME_PATH);
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
