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

<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} = <${varNameType.second}> recall_var("${varNameType.first?j_string}");
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
    <#list saveVarDclns as varNameType>
    memorize_var("${varNameType.first?j_string}", ${varNameType.first});
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

public function detect_errors_ahead() {
    // This will detect errors that will occur in the next iteration.
    // Essentially detecting errors ahead. Without this, state corruption is possible.
    <#list saveVarDclns as varNameType>
    var
    ${varNameType.first}
     = <
    ${varNameType.second}
    > recall_var("");
    </#list>
}

