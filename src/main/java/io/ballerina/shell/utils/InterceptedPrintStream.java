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

package io.ballerina.shell.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * Print stream interceptor which would intercept print calls
 * and will add a intercepting consumer to send the string representation.
 */
public class InterceptedPrintStream extends PrintStream {
    private final Consumer<String> stringConsumer;

    public InterceptedPrintStream(OutputStream alternate, Consumer<String> stringConsumer) {
        super(alternate, true, Charset.defaultCharset());
        this.stringConsumer = stringConsumer;
    }

    @Override
    public void print(String s) {
        super.print(s);
        stringConsumer.accept(s);
    }

    @Override
    public void println(String s) {
        super.println(s);
        stringConsumer.accept(s + System.lineSeparator());
    }
}
