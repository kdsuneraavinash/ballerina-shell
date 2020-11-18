<#-- @ftlvariable name="" type="io.ballerina.shell.executor.reeval.ReEvalContext" -->
import ballerina/crypto;
import ballerina/encoding;
import ballerina/io;
import ballerina/java;
import ballerina/java.arrays as jarrays;
import ballerina/jsonutils;
import ballerina/jwt;
import ballerina/math;
import ballerina/reflect;
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/system;
import ballerina/time;
import ballerina/xmlutils;
import ballerina/xslt;

// External import statements
<#list imports as import>
    ${import}
</#list>

// Module/Variable declarations
<#list moduleDclns + varDclns as topDcln>
    ${topDcln}
</#list>

# Old statements will be added here.
# Since old statements are checked, no error should be returned.
function _old_statements() {
    <#list stmts as stmt>
        <#if stmt.expr>
            _reserved = ${stmt.code}; // Expressions as valid statements
        <#elseif stmt.stmt>
            ${stmt.code} // Statements as is
        </#if>
    </#list>
}

# New statements will be added here.
# These might be invalid/runtime error statements.
# + return Error if any old statement caused an error.
function _new_statement() returns any|error {
    <#if lastStmt.stmt>
        ${lastStmt.code}
    </#if>
    _reserved = ();
    <#if lastStmt.expr>
        _reserved = ${lastStmt.code};
    </#if>
    return _reserved;
}

// _reserved declarations.
any|error _reserved = ();

# Main function entry point.
# To filter old print statements, a filter guard is set.
public function main() {
    // Run old statements.
    // Inside a IO guard,
    //      Evaluate current expression/statement
    //      If return value is null, ignore
    //      If runtime error, Output colored error message
    //      Otherwise print string representation

    _old_statements();

    io:println();
    io:println("${ioActivationStart}");

    any|error expr = trap _new_statement();

    if (expr is ()){
    } else if (expr is error){
        var color_start = "\u{001b}[33;1m";
        var color_end = "\u{001b}[0m";
        io:println(color_start, "Exception occurred: ", expr.message(), color_end);
    } else {
        io:println(expr);
    }

    io:println("${ioActivationEnd}");
}

# Useless function to pretend to use imports so the
# compiler won't complain.
function _pretend_to_use_imports() {
    _reserved = crypto:Error;
    _reserved = encoding:Error;
    _reserved = java:toString;
    _reserved = jarrays:fromHandle;
    _reserved = jsonutils:fromTable;
    _reserved = jwt:Error;
    _reserved = math:Error;
    _reserved = reflect:getServiceAnnotations;
    _reserved = runtime:sleep;
    _reserved = stringutils:contains;
    _reserved = system:Error;
    _reserved = time:Error;
    _reserved = xmlutils:fromJSON;
    _reserved = xslt:transform;
    _reserved = ();
}
