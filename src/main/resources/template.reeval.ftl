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

// Declare variables to monitor state change
// (we need a value to declare, so use initial values)
<#list varNames as varName>
    var _${varName} = ${varName};
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

    // Store current state after old stmts
    <#list varNames as varName>
        _${varName} = ${varName};
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

function _restore() {
    // restore variables here
}


// If non serializable state didnt change,
// then changed state was serialized
function _is_state_same_or_preserved() returns boolean {
    return
    <#list nonSerializedVarNames as varName>
        _${varName} == ${varName} &&
    </#list> !${ifNewNonSerializableVar?c};
}

function _dump() {
    json repr = {
        vars: _json_repr({
            // Variables here
            <#list serializedVarNames as varName>
                ${varName} <#sep>,
            </#list>
        }),
        isStmtPreserved: _is_state_same_or_preserved()
    };
    var result = _write(repr, "state.dump");
}

// =========================================
// === Functionality Snippets ==============
// =========================================

// _reserved declarations.
any|error _reserved = ();
type _everytype any|error;

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
    _dump();

    io:println("${ioActivationEnd}");
}

# writes a JSON to the given file.
#
# + content - JSON content to write.
# + path - Path to write the JSON file to. This file should be empty or non-existing.
# + return - Error if there was any.
function _write(json content, string path) returns error? {
    io:WritableByteChannel wbc = check io:openWritableFile(${r'<@untainted>'} path);
    io:WritableCharacterChannel wch = new (wbc, "UTF8");
    var result = wch.writeJson(content);
    var close_res = wch.close();
    return result;
}

# Join a string array by a delimeter.
#
# + delimeter - String to use for joining.
# + list - String array to join.
# + return - Joined string representation.
function _join_arr(string delimeter, string[] list) returns string {
    var joiner = function(string full, string now) returns string => full + delimeter + now;
    string joined = list.reduce(joiner, "");
    if joined.length() < delimeter.length() {
        return joined;
    }
    // remove first delimiter
    return joined.substring(delimeter.length());
}

# Converts value to a string representation of serializable type.
# The returned representation should be a initializer that
# can be used to give a variable the said value.
# For example, "hello" is converted to "\"Hello\"".
# An error is thrown if representation failed.
#
# + value - Value to represent.
# + return - Representation. Error if failed.
function _str_repr(_everytype value) returns string {
    if value is () { // nil
        return "()";
    } else if value is boolean|int|float|decimal { // boolean, int, float, decimal
        return io:sprintf("%s", value);
    } else if value is string { // string
        return string ${r'`"${value}"`'};
    } else if value is xml { // xml
        return io:sprintf("xml `%s`", value);
    } else if value is _everytype[] { // list
        string[] mapped = [];
        foreach int i in 0 ..< value.length() {
            mapped.push(_str_repr(value[i]));
        }
        return "[" + _join_arr(",", mapped) + "]";
    } else if (value is map<_everytype>) { // map
        string[] mapped = [];
        foreach [string, _everytype] [k, v] in value.entries() {
            string mapped_v = _str_repr(v);
            mapped.push(k + ":" + mapped_v);
        }
        return "{" + _join_arr(",", mapped) + "}";
    } else {
        return "";
    }
}

# Converts a variable declaration map into the json format.
# An error is thrown if representation failed.
#
# + vars - Variable declarations map. Should follow `{..., vari: vari_val, ...}` format.
# + return - The json representation of the variable declaration. Error if failed.
function _json_repr(map<_everytype> vars) returns @untainted map<string> {
    map<string> mapped = {};
    foreach [string, _everytype] [name, value] in vars.entries() {
        mapped[name]  = _str_repr(value);
    }
    return mapped;
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
