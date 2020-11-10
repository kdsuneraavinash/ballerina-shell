import ballerina/io;
%s
%s
function stmts() returns error? {
%s
}
function do_it() returns string|error {
    check stmts();
    any|error expr = %s;
    any value = checkpanic expr;
    if (expr is ()) {
        return io:sprintf("%%s", value);
    }
    return io:sprintf("%%s\n", value);
}
public function main() {
    string|error result = trap do_it();
    if (result is string) {
        io:println(result);
    } else {
        io:println("Error occurred: ", result.message());
    }
}
