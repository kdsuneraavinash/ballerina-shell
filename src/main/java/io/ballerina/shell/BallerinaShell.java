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
package io.ballerina.shell;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.shell.executor.DirectExecutor;
import io.ballerina.shell.executor.Executor;
import io.ballerina.shell.postprocessor.BasicPostProcessor;
import io.ballerina.shell.preprocessor.Preprocessor;
import io.ballerina.shell.preprocessor.StatementSeparatorPreprocessor;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.transformer.MasterTransformer;
import io.ballerina.shell.treeparser.TreeParser;
import io.ballerina.shell.treeparser.TrialTreeParser;
import io.ballerina.shell.wrapper.TemplateWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main shell entry point.
 */
public class BallerinaShell {
    private static final Logger LOGGER = Logger.getLogger(BallerinaShell.class.getName());

    private final PrintStream outStream;
    private final PrintStream errStream;

    private final Preprocessor preprocessor;
    private final TreeParser parser;
    private final MasterTransformer transformer;
    private final Executor executor;

    public BallerinaShell(PrintStream outStream, PrintStream errStream) {
        this.outStream = outStream;
        this.errStream = errStream;
        this.preprocessor = new StatementSeparatorPreprocessor();
        this.parser = new TrialTreeParser();
        this.transformer = new MasterTransformer();
        this.executor = new DirectExecutor(new TemplateWrapper(), new BasicPostProcessor());
    }

    public void evaluate(String input) {
        try {
            List<String> source = preprocessor.preprocess(input);
            for (String sourceLine : source) {
                Node rootNode = parser.parse(sourceLine);
                List<Snippet<?>> snippets = transformer.transform(rootNode);
                for (Snippet<?> snippet : snippets) {
                    String output = executor.execute(snippet);
                    outStream.println(output);
                }
            }
        } catch (Exception e) {
            errStream.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Reader reader = new InputStreamReader(System.in, Charset.defaultCharset());
        BallerinaShell shell = new BallerinaShell(System.out, System.err);
        BufferedReader bufferedReader = new BufferedReader(reader);

        String input;
        System.out.print(">> ");
        while ((input = bufferedReader.readLine()) != null) {
            try {
                shell.evaluate(input);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            } finally {
                System.out.print(">> ");
            }
        }
        bufferedReader.close();
    }
}
