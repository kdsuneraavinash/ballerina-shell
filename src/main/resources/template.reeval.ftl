<#-- @ftlvariable name="" type="io.ballerina.shell.executor.reeval.ReEvalContext" -->
import ballerina/io;

// Import statements
<#list imports as import>
    ${import}
</#list>

// Module/Variable declarations
<#list moduleDclns + varDclns as topDcln>
    ${topDcln}
</#list>

# Old statements will be added here.
# Since old statements are checked, no error should be returned.
function old_statements() {
    <#list stmts as stmt>
        <#if stmt.expr>
            reserved = ${stmt.code}; // Expressions as valid statements
        <#elseif stmt.stmt>
            ${stmt.code} // Statements as is
        </#if>
    </#list>
}

# New statements will be added here.
# These might be invalid/runtime error statements.
# + return Error if any old statement caused an error.
function new_statement() returns error? {
    <#if lastStmt.stmt>
        ${lastStmt.code}
    </#if>
}

// Reserved declarations.
any|error reserved = ();
type NoExpression record {};
type NoExpressionError error<NoExpression>;

# Main run function.
# Will execute new statements and expressions.
# If there is a runtime error in any expressions/statements,
# an error would be returned.
# Otherwise a string representation of the expression will be returned.
# If last statement is not an expression, `NoExpressionError` is thrown.
# + return String representation if OK, otherwise the error.
function run() returns string|error {
    check  new_statement();
    any|error expr =  <#if lastStmt.expr> ${lastStmt.code} <#else> NoExpressionError("No expression") </#if>;
    any value = checkpanic expr;
    string output = io:sprintf("%s", value);
    return output;
}

# Main function entry point.
# To filter old print statements, a filter guard is set.
public function main() {
    old_statements();

    io:println();
    io:println("${ioActivationStart}"); // Begin IO guard

    string|error result = trap run();
    if (result is string) {
        // Output expression evaluation
        io:println(result);
    } else if (result is NoExpressionError) {
        // Ignore if no expression given
    }else {
        // Output colored error message if runtime error thrown
        var color_start = "\u{001b}[33;1m";
        var color_end = "\u{001b}[0m";
        io:println(color_start, "Exception occurred: ", result.message(), color_end);
    }

    io:println("${ioActivationEnd}"); // End IO guard
}

# Useless function to pretend to use imports so the
# compiler won't complain.
# This will accept imports with `Error` exported.
# Most of the standard modules export `Error`.
# TODO: Remove this temp fix.
function garbage_function() {
    <#list importPrefixes as importPrefix>
        reserved = ${importPrefix}:Error;
    </#list>
}
