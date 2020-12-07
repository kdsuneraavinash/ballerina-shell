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
${lastVarDcln}

<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} = <${varNameType.second}> recall_var("${varNameType.first}");
</#list>

function save() {
    <#list saveVarDclns as varNameType>
    memorize_var("${varNameType.first}", ${varNameType.first});
    </#list>
}

function run() returns @untainted any|error {
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

public function main() returns error? {
    any|error ${exprVarName} = trap run();
     if (${exprVarName} is error){
        io:println("Exception occurred: ", ${exprVarName});
        return ${exprVarName};
    }
    memorize_var("${exprVarName}", ${exprVarName});
    return trap save();
}
