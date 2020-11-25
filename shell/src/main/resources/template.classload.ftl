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

// can use as a normal identifier
function recall_var(string name) returns any|error {
    return trap recall(context_id, java:fromString(name));
}

// can use as an assignment
function memorize_var(string name, any|error value) {
    memorize(context_id, java:fromString(name), value);
}

// Module level declarations
<#list moduleDclns as dcln>
${dcln}
</#list>

handle context_id = java:fromString("${contextId}");
${lastVarDcln}

// Variable initialization - recall and cast
<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} = <${varNameType.second}> recall_var("${varNameType.first}");
</#list>

// Saving variables
function save(){
<#list saveVarDclns as varNameType>
    memorize_var("${varNameType.first}", ${varNameType.first});
</#list>
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
    any|error expr = trap run();
    if (expr is ()){
    } else if (expr is error){
        io:println("Exception occurred: ", expr.message());
        return expr;
    } else {
        io:println(expr);
    }

    return trap save();
}
