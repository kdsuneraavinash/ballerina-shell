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

public function main() {
    <#list initVarDclns as varNameType>
    ${varNameType.second} ${varNameType.first} = // value
    <${varNameType.second}> recall_var("x");
    </#list>

    ${lastVarDcln}

    io:println("Hello world");
    _ = java:JavaClassNotFoundError;
}
