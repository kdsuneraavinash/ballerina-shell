<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.classload.ClassLoadContext" -->
import ballerina/io as io;
import ballerina/java as java;
<#list imports as import>
    ${import}
</#list>

function recall_var(string name) returns any|error { return (); }
function memorize_var(string name, any|error value) { }

<#list moduleDclns as dcln>
${dcln}
</#list>

${lastVarDcln}
<#list initVarDclns as varNameType>
${varNameType.second} ${varNameType.first} =
    <${varNameType.second}> recall_var("x");
</#list>

public function main(){
    io:println("Hello world");
    _ = java:JavaClassNotFoundError;
}
