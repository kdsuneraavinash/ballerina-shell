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

package io.ballerina.shell.test.evaluator.base;

import io.ballerina.shell.invoker.classload.ClassLoadInvoker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

/**
 * Class load invoker made testable.
 */
public class TestInvoker extends ClassLoadInvoker {
    private String output = "";

    @Override
    protected int invokeMethod(Method method) throws IllegalAccessException {
        PrintStream stdOut = System.out;
        ByteArrayOutputStream stdOutBaOs = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(stdOutBaOs, true, Charset.defaultCharset()));
            return super.invokeMethod(method);
        } finally {
            this.output = stdOutBaOs.toString(Charset.defaultCharset());
            this.output = this.output.replace("\r\n", "\n");
            System.setOut(stdOut);
        }
    }

    public String getOutput() {
        return output;
    }
}
