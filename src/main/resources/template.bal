import ballerina/io;

// Imports here
%s

// Module member declaration here
%s

// Variable declarations here
%s

function stmts() returns error? {
// If statement here
%s
}

function run() returns string|error {
    check stmts();
    any|error expr = %s; // If expression here
    any value = checkpanic expr;
    string output = io:sprintf("%%s", value);
    return output;
}

public function main() {
    string|error result = trap run();
    if (result is string) {
        io:println(result);
    } else {
        io:println("\u{001b}[33;1mException occurred: ", result.message(), "\u{001b}[0m");
    }
}
