<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.classload.ClassLoadContext" -->
import ballerina/io as io;
import ballerina/java;

// Other imports
<#list imports as import>
${import}
</#list>

function recall(handle name) returns any|error = @java:Method {
    'class: "io.ballerina.shell.invoker.classload.ClassLoadMemory"
} external;

function memorize(handle name, any|error value) = @java:Method {
    'class: "io.ballerina.shell.invoker.classload.ClassLoadMemory"
} external;

// Module level declarations
<#list moduleDclns as dcln>
${dcln}
</#list>

${lastVarDcln}

// Variable initialization
<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} = <${varNameType.second}> recall(java:fromString("${varNameType.first}"));
</#list>

// Saving variables
function save(){
<#list saveVarDclns as varNameType>
    memorize(java:fromString("${varNameType.first}"), ${varNameType.first});
</#list>
}

// Helper: print error message
function print_err(error err){
    var color_start = "\u{001b}[33;1m";
    var color_end = "\u{001b}[0m";
    io:println(color_start, "Exception occurred: ", err.message(), color_end);
}

public function main() {
    any|error expr = ();
    <#if lastExpr.second>
    ${lastExpr.first}
    <#else>
    expr = trap (${lastExpr.first});
    </#if>

    if (expr is ()){
    } else if (expr is error){
        print_err(expr);
    } else {
        io:println(expr);
    }

    error? err = trap save();
    if (err is error){
        print_err(err);
    }
}
