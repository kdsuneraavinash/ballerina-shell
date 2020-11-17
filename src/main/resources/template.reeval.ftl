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
function new_statement() returns any|error {
    <#if lastStmt.stmt>
        ${lastStmt.code}
    </#if>
    reserved = ();
    <#if lastStmt.expr>
        reserved = ${lastStmt.code};
    </#if>
    return reserved;
}

// Reserved declarations.
any|error reserved = ();

# Main function entry point.
# To filter old print statements, a filter guard is set.
public function main() {
    old_statements();

    io:println();
    io:println("${ioActivationStart}"); // Begin IO guard

    any|error expr = trap new_statement();

    // If null, ignore
    // If runtime error, Output colored error message
    // Others, print string representation
    if (expr is ()){
    } else if (expr is error){
        var color_start = "\u{001b}[33;1m";
        var color_end = "\u{001b}[0m";
        io:println(color_start, "Exception occurred: ", expr.message(), color_end);
    } else {
        io:println(expr);
    }

    io:println("${ioActivationEnd}"); // End IO guard
}

// =========================================
// === Functionality Snippets ==============
// =========================================

# Useless function to pretend to use imports so the
# compiler won't complain.
function pretend_to_use_imports() {
    reserved = crypto:Error;
    reserved = encoding:Error;
    reserved = java:toString;
    reserved = jarrays:fromHandle;
    reserved = jsonutils:fromTable;
    reserved = jwt:Error;
    reserved = math:Error;
    reserved = reflect:getServiceAnnotations;
    reserved = runtime:sleep;
    reserved = stringutils:contains;
    reserved = system:Error;
    reserved = time:Error;
    reserved = xmlutils:fromJSON;
    reserved = xslt:transform;
    reserved = ();
}
