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

package io.ballerina.shell.test.unit.invoker;

import io.ballerina.shell.exceptions.BallerinaShellException;
import io.ballerina.shell.invoker.classload.ClassLoadInvoker;
import io.ballerina.shell.test.TestUtils;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

/**
 * Class with helper function to directly evaluate the evaluator.
 * These tests will evaluate the evaluator directly without snippet creation.
 */
public abstract class AbstractInvokerTest {
    private static class TestInvoker extends ClassLoadInvoker {
        private String output;

        @Override
        protected int invokeMethod(Method method) throws IllegalAccessException {
            PrintStream stdOut = System.out;
            ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
            try {
                System.setOut(new PrintStream(stdOutBaOs, true, Charset.defaultCharset()));
                return super.invokeMethod(method);
            } finally {
                this.output = stdOutBaOs.toString(Charset.defaultCharset());
                System.setOut(stdOut);
            }
        }
    }

    /**
     * Evaluates a source from a file and compares with the stdout given.
     *
     * @param fileName   File to run.
     * @param stdOutFile File with expected stdout.
     * @throws BallerinaShellException If execution failed.
     */
    protected void testEvaluate(String fileName, String stdOutFile) throws BallerinaShellException {
        try {
            TestInvoker invoker = new TestInvoker();
            String content = TestUtils.readFile(fileName);
            String stdOut = TestUtils.readFile(stdOutFile);
            boolean isSuccess = invoker.execute(content).getFirst();
            Assert.assertTrue(isSuccess, fileName);
            Assert.assertEquals(removeCR(invoker.output), removeCR(stdOut), fileName);
        } catch (BallerinaShellException e) {
            Assert.fail(fileName, e);
        }
    }

    private static String removeCR(String string) {
        return string.replace("\r\n", "\n");
    }
}
