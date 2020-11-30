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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.exceptions.SnippetException;
import io.ballerina.shell.exceptions.TreeParserException;
import io.ballerina.shell.parser.TreeParser;
import io.ballerina.shell.parser.TrialTreeParser;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.factory.BasicSnippetFactory;
import io.ballerina.shell.snippet.factory.SnippetFactory;
import io.ballerina.shell.test.TestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test snippet factory use cases.
 */
public class BasicSnippetFactoryTest {
    private static final String IMPORT_TESTCASES = "testcases/snippet.import.json";
    private static final String EXPRESSION_TESTCASES = "testcases/snippet.expression.json";
    private static final String MODULEDCLN_TESTCASES = "testcases/snippet.moduledcln.json";
    private static final String STATEMENT_TESTCASES = "testcases/snippet.statement.json";
    private static final String VARDCLN_TESTCASES = "testcases/snippet.vardcln.json";

    private static class TestCase {
        String name;
        String input;
        boolean accepted;
    }

    private static class TestCases extends ArrayList<TestCase> {
    }

    @Test
    public void testCreateImportSnippet() {
        testProcess(IMPORT_TESTCASES, SnippetKind.IMPORT_DECLARATION);
    }

    @Test
    public void testCreateVariableDeclarationSnippet() {
        testProcess(VARDCLN_TESTCASES, SnippetKind.VARIABLE_DECLARATION);
    }

    @Test
    public void testCreateModuleMemberDeclarationSnippet() {
        testProcess(MODULEDCLN_TESTCASES, SnippetKind.MODULE_MEMBER_DECLARATION);
    }

    @Test
    public void testCreateStatementSnippet() {
        testProcess(STATEMENT_TESTCASES, SnippetKind.STATEMENT);
    }

    @Test
    public void testCreateExpressionSnippet() {
        testProcess(EXPRESSION_TESTCASES, SnippetKind.EXPRESSION);
    }

    private void testProcess(String fileName, SnippetKind kind) {
        // TODO: Improve this test to check for the sub kinds, errors and ignores
        //  after implementing them in the Snippets.
        List<TestCase> testCases = TestUtils.loadTestCases(fileName, TestCases.class);
        TreeParser treeParser = new TrialTreeParser();
        SnippetFactory snippetFactory = new BasicSnippetFactory();
        for (TestCase testCase : testCases) {
            try {
                Node node = treeParser.parse(testCase.input);
                Snippet snippet = snippetFactory.createSnippet(node);
                Assert.assertNotNull(snippet, testCase.name);
                Assert.assertTrue(testCase.accepted, testCase.name);
                Assert.assertEquals(snippet.getKind(), kind, testCase.name);
            } catch (TreeParserException e) {
                Assert.fail(testCase.name + " error: " + e);
            } catch (SnippetException e) {
                Assert.assertFalse(testCase.accepted, testCase.name);
            }
        }
    }
}
