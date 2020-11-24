<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.classload.ClassLoadContext" -->
import ballerina/io as io;
import ballerina/java as java;

// Other imports
<#list imports as import>
${import}
</#list>

function recall(handle context_id, handle name) returns any|error = @java:Method {
    'class: "${memoryRef}"
} external;

function memorize(handle context_id, handle name, any|error value) = @java:Method {
    'class: "${memoryRef}"
} external;

// Module level declarations
<#list moduleDclns as dcln>
${dcln}
</#list>

handle context_id = java:fromString("${contextId}");
${lastVarDcln}

// Variable initialization
<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} = <${varNameType.second}> recall(context_id, java:fromString("${varNameType.first}"));
</#list>

// Saving variables
function save(){
<#list saveVarDclns as varNameType>
    memorize(context_id, java:fromString("${varNameType.first}"), ${varNameType.first});
</#list>
}

function print_err(error err){
    io:println("Exception occurred: ", err.message());
}

function run() returns @untainted any|error {
    any|error expr = ();
    <#if lastExpr.second>
    ${lastExpr.first}
    <#else>
    expr = trap (${lastExpr.first});
    </#if>
    return expr;
}

public function main() returns error? {
    any|error expr = run();
    if (expr is ()){
    } else if (expr is error){
        print_err(expr);
        return expr;
    } else {
        io:println(expr);
    }

    return trap save();
}
