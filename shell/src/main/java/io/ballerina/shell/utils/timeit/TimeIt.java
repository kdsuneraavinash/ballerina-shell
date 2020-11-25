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

package io.ballerina.shell.utils.timeit;

import io.ballerina.shell.Diagnostic;
import io.ballerina.shell.DiagnosticReporter;
import io.ballerina.shell.exceptions.BallerinaShellException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

/**
 * Utility calls for performance measurements.
 */
public class TimeIt {

    private static final HashMap<String, OperationTimeEntry> operationTimes = new HashMap<>();

    /**
     * Times the operation and sends a debug message.
     * Statistics will also be calculated.
     *
     * @param owner     Diagnostic owner. The time debug data would be emitted to this class.
     * @param operation Operation callback to run.
     * @throws BallerinaShellException If the operation failed.
     */
    public static <T> T timeIt(DiagnosticReporter owner, TimedOperation<T> operation) throws BallerinaShellException {
        String name = owner.getClass().getSimpleName();

        Instant start = Instant.now();
        T result = operation.run();
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        OperationTimeEntry entry = operationTimes.getOrDefault(name, new OperationTimeEntry());
        entry.addDuration(duration);
        Duration mean = entry.mean();
        operationTimes.put(name, entry);

        String message = String.format("%s took %s ms. Average is %s ms.", name, duration.toMillis(), mean.toMillis());
        owner.addDiagnostic(Diagnostic.debug(message));

        return result;
    }
}
