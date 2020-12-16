<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.classload.ClassLoadContext" -->
import ballerina/io as io;
import ballerina/java as java;

<#list imports as import>
${import}
</#list>

function recall(handle context_id, handle name) returns any|error = @java:Method {
    'class: "${memoryRef}"
} external;
function memorize(handle context_id, handle name, any|error value) = @java:Method {
    'class: "${memoryRef}"
} external;
function recall_var(string name) returns any|error {
    return trap recall(context_id, java:fromString(name));
}
function memorize_var(string name, any|error value) {
    memorize(context_id, java:fromString(name), value);
}

<#list moduleDclns as dcln>
${dcln}
</#list>

handle context_id = java:fromString("${contextId}");

<#list varDclns as varDcln>
<#if varDcln.new>
(${varDcln.type})? ${varDcln.name} = (); // There is an issue with the name or type
<#else>
${varDcln.type} ${varDcln.name} = <${varDcln.type}> recall_var("${varDcln.name?j_string}");
</#if>
</#list>

function run() returns @untainted any|error {
    // Will run current statement/expression and return its result.
    <#if lastExpr.second>
    if (true) {
        ${lastExpr.first}
    }
    return ();
    <#else>
    return trap (
    ${lastExpr.first}
    );
    </#if>
}

public function stmts() returns any|error {
    // This will execute the statement and initialize and save var dcln.
    // The variable is declared in local context to enable various expressions.
    any|error ${exprVarName} = trap run();
    ${lastVarDcln}
    memorize_var("${exprVarName?j_string}", ${exprVarName});
    <#list varDclns as varDcln>
    memorize_var("${varDcln.name?j_string}", ${varDcln.name});
    </#list>
    return ${exprVarName};
}

public function main() returns error? {
    any|error ${exprVarName} = trap stmts();
     if (${exprVarName} is error){
        io:println("Exception occurred: ", ${exprVarName});
        return ${exprVarName};
    }
}
