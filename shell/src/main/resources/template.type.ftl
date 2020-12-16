<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.classload.ClassLoadContext" -->
import ballerina/io as io;
import ballerina/java as java;
<#list imports as import>
    ${import}
</#list>

<#list moduleDclns as dcln>
${dcln}
</#list>

function recall_var(string name) returns any|error { return (); }
function memorize_var(string name, any|error value) { }

<#list varDclns as varDcln>
${varDcln.type} ${varDcln.name} = // value
<${varDcln.type}> recall_var("x");
</#list>

public function main() returns error? {
    // Redefine to restrict user
    <#list varDclns as varDcln>
    <#if !varDcln.new>
    ${varDcln.type} ${varDcln.name} = // value
    <${varDcln.type}> recall_var("x");
    </#if>
    </#list>

    ${lastVarDcln}

    io:println("Hello world");
    _ = java:JavaClassNotFoundError;
}
