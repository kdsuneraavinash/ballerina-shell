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
import io.ballerina.shell.invoker.replay.ReplayInvoker;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.preprocessor.SeparatorPreprocessor;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class AbstractEvaluatorTest {
    private static final Path BALLERINA_RUNTIME = Paths.get("../home/bre/lib/*");
    private static final Path BALLERINA_HOME_PATH = Paths.get("../home");

    private static final String TEMPLATE_FILE = "template.replay.ftl";
    private static final String TEMP_FILE_NAME = "main.bal";

    private static class TestCaseLine {
        String code;
        String output = "";
    }

    private static class TestCase extends ArrayList<TestCaseLine> {
    }

    private static class TestInvoker extends ReplayInvoker {
        private String output;

        public TestInvoker(String templateName, String tmpFileName, Path ballerinaRuntime, Path ballerinaHome) {
            super(templateName, tmpFileName, ballerinaRuntime, ballerinaHome);
        }

        @Override
        protected int runCommand(List<String> commands) throws IOException, InterruptedException {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(commands.toArray(new String[0]));
            process.waitFor();
            Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
            this.output = scanner.hasNext() ? scanner.next() : "";
            return process.exitValue();
        }
    }

    protected void testEvaluate(String fileName) throws BallerinaShellException {
        TestInvoker invoker = getInvoker();
        Evaluator evaluator = getEvaluator(invoker);
        TestCase testCase = TestUtils.loadTestCases(fileName, TestCase.class);
        for (TestCaseLine testCaseLine : testCase) {
            evaluator.evaluate(testCaseLine.code);
            Assert.assertEquals(invoker.output, testCaseLine.output);
        }
    }

    private TestInvoker getInvoker() {
        return new TestInvoker(
                TEMPLATE_FILE, TEMP_FILE_NAME,
                BALLERINA_RUNTIME, BALLERINA_HOME_PATH);
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
