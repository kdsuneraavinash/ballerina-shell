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

package io.ballerina.shell.unit;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.shell.TestUtils;
import io.ballerina.shell.exceptions.TreeParserException;
import io.ballerina.shell.parser.TreeParser;
import io.ballerina.shell.parser.TrialTreeParser;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class TrialTreeParserTest {
    private static final String STATEMENT_TESTCASES = "testcases/treeparser.statement.json";
    private static final String IMPORT_TESTCASES = "testcases/treeparser.import.json";
    private static final String EXPRESSION_TESTCASES = "testcases/treeparser.expression.json";
    private static final String MODULE_DCLN_TESTCASES = "testcases/treeparser.moduledcln.json";

    private static class TestCase {
        String name;
        String input;
        String expected;
    }

    private static class TestCases extends ArrayList<TestCase> {
    }

    @Test
    public void testImportParse() {
        // TODO: Add test cases with versions
        testParse(IMPORT_TESTCASES, ImportDeclarationNode.class);
    }

    @Test
    public void testModuleMemberDeclarationParse() {
        // TODO: Add test cases with Annotation Declaration
        // Enums not tested: Enum dclns go into stack overflow
        // {
        //    "name": "Enum Declaration",
        //    "input": "enum Color { RED, GREEN, BLUE };",
        //    "expected": "EnumDeclarationNode"
        //  }
        testParse(MODULE_DCLN_TESTCASES, ModuleMemberDeclarationNode.class);
    }

    @Test
    public void testStatementParse() {
        // In this test cases, Some statements are not tested;
        //  - Expression Statement - These are caught as an expression
        //  - Local Type Definition Statement - These are caught as a module level dcln
        //  - XML Namespace Declaration Statement - These are caught as a module level dcln
        //  - Module Variable Declaration - These are caught as a module level dcln
        // TODO: Panic statements not tested: Panic statements go into stack overflow (without semicolon)
        //  {
        //    "name": "Panic Statement",
        //    "input": "panic error(\"Record is nil\");",
        //    "expected": "PanicStatementNode"
        //   }
        testParse(STATEMENT_TESTCASES, StatementNode.class);
    }

    @Test
    public void testExpressionParse() {
        testParse(EXPRESSION_TESTCASES, ExpressionNode.class);
    }

    private void testParse(String fileName, Class<?> parentClazz) {
        TestCases testCases = TestUtils.loadTestCases(fileName, TestCases.class);
        TreeParser treeParser = new TrialTreeParser();
        for (TestCase testCase : testCases) {
            try {
                Node node = treeParser.parse(testCase.input);
                String actual = node.getClass().getSimpleName();
                Assert.assertEquals(actual, testCase.expected, testCase.name);
                Assert.assertTrue(parentClazz.isInstance(node), testCase.name + " not expected instance");
            } catch (TreeParserException e) {
                Assert.assertNull(testCase.expected, testCase.name + " error: " + e.getMessage());
            }
        }
    }
}
