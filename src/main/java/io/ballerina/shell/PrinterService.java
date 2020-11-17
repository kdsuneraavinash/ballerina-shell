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

/**
 * Interface of a callback to output debug messages.
 */
public interface PrinterService {
    /**
     * Adds a shell results to the output.
     * Child classes may override this to fetch the shell results.
     * Every result, including errors are emitted via this interface,
     *
     * @param output Output string.
     * @param status Status of the output.
     */
    void write(String output, LogStatus status);
}
