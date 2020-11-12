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
package io.ballerina.shell.executor;

import com.google.gson.Gson;
import io.ballerina.shell.diagnostics.ShellDiagnosticProvider;
import io.ballerina.shell.executor.process.ProcessInvoker;
import io.ballerina.shell.executor.process.ShellProcessInvoker;
import io.ballerina.shell.executor.wrapper.SourceGenWrapper;
import io.ballerina.shell.snippet.Snippet;
import io.ballerina.shell.snippet.SnippetKind;
import io.ballerina.shell.snippet.StatementSnippet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * An executor that will delegate the process to the external ballerina executable command.
 * Ballerina must be installed and on path to use this executor.
 * This would directly call the ballerina command.
 * This would generate the source code so that the ballerina code will dump its state.
 */
public class SourceGenExecutor extends StatefulExecutor {
    private static class ExecutorState extends HashMap<String, String> {
    }

    private static final String BALLERINA_COMMAND = "ballerina run %s %s";
    private static final String GENERATED_FILE = "main.bal";
    private static final String DUMP_FILE = "state.dump";
    private final ProcessInvoker processInvoker;
    protected ArrayList<Snippet<?>> preservedSnippets;
    private final Gson gson;
    private ExecutorState state;

    public SourceGenExecutor() {
        super(new SourceGenWrapper());
        String command = String.format(BALLERINA_COMMAND, GENERATED_FILE, DUMP_FILE);
        processInvoker = new ShellProcessInvoker(command);
        ShellDiagnosticProvider.sendMessage("Using source gen executor with shell process invoker.");
        ShellDiagnosticProvider.sendMessage("Shell command invocation used: " + command);
        preservedSnippets = new ArrayList<>();
        state = new ExecutorState();
        gson = new Gson();
    }

    @Override
    protected ExecutorResult evaluateSourceCode(String sourceCode) {
        File generatedFile = new File(GENERATED_FILE);
        File stateDumpFile = new File(DUMP_FILE);

        try (FileWriter fileWriter = new FileWriter(generatedFile, Charset.defaultCharset())) {
            fileWriter.write(sourceCode);
        } catch (IOException e) {
            throw new RuntimeException("Target file write failed.", e);
        }

        // Delete state file
        if (stateDumpFile.delete()) {
            ShellDiagnosticProvider.sendMessage("Previous state dump file deleted.");
        }

        // Execute and return correct output.
        ExecutorResult result;
        try {
            processInvoker.execute();
            List<String> standardStrings = processInvoker.isErrorExit()
                    ? processInvoker.getStandardError()
                    : processInvoker.getStandardOutput();
            String output = String.join("\n", standardStrings);
            result = new ExecutorResult(processInvoker.isErrorExit(), output);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException("Ballerina process invocation failed.", e);
        }

        // Restore state
        if (!result.isError()) {
            state = getStateAfterwards(stateDumpFile);
        }

        return Objects.requireNonNull(result);
    }

    @Override
    protected List<Snippet<?>> getSnippetsForExecution(Snippet<?> newSnippet) {
        List<Snippet<?>> generated = new ArrayList<>();
        for (String identifier : state.keySet()) {
            String sourceCode = String.format("%s = %s;", identifier, state.get(identifier));
            generated.add(StatementSnippet.fromCodeOfAssignment(sourceCode));
        }
        if (shouldPreserve(newSnippet)) {
            ShellDiagnosticProvider.sendMessage("Preserving new snippet.");
            preservedSnippets.add(newSnippet);
            generated.addAll(preservedSnippets);
        } else {
            generated.addAll(preservedSnippets);
            generated.add(newSnippet);
        }
        ShellDiagnosticProvider.sendMessage("Has " + preservedSnippets.size() + " snippets");
        return generated;
    }

    @Override
    protected void executionSuccessful(Snippet<?> newSnippet) {
        ShellDiagnosticProvider.sendMessage("Execution succeeded.");
        // Do nothing
    }

    @Override
    protected void executionFailed(Snippet<?> newSnippet) {
        if (shouldPreserve(newSnippet)) {
            ShellDiagnosticProvider.sendMessage("Execution succeeded.");
            preservedSnippets.remove(newSnippet);
        }
    }

    /**
     * Read the afterwards state from the state file.
     * If the last execution was a success, this call should be successful.
     *
     * @param stateFile State dump file reference.
     * @return State object of the ballerina execution.
     */
    private ExecutorState getStateAfterwards(File stateFile) {
        // Read state data
        try (FileReader fileReader = new FileReader(stateFile, Charset.defaultCharset())) {
            Scanner scanner = new Scanner(fileReader);
            String data = scanner.nextLine();
            ExecutorState newState = gson.fromJson(data, ExecutorState.class);
            ShellDiagnosticProvider.sendMessage("New state: " + newState);
            return newState;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read state file.", e);
        }
    }

    /**
     * Snippets that should be preserved in this context.
     *
     * @param snippet Snippet.
     * @return Whether it should be preserved.
     */
    private boolean shouldPreserve(Snippet<?> snippet) {
        return snippet.getKind() == SnippetKind.IMPORT_KIND
                || snippet.getKind() == SnippetKind.MODULE_MEMBER_DECLARATION_KIND
                || snippet.getKind() == SnippetKind.VARIABLE_DEFINITION_KIND;
    }
}
