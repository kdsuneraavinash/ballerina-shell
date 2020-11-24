<#-- @ftlvariable name="" type="io.ballerina.shell.invoker.replay.ReplayContext" -->
import ballerina/io as _io;
<#list imports as import>
    ${import}
</#list>

<#list varDclns + moduleDclns as dcln>
    ${dcln}
</#list>

function statements() {
    any|error _reserved = ();
    <#list stmts as stmt>
        <#if stmt.second>
           ${stmt.first}
        <#else>
           _reserved = (${stmt.first});
        </#if>
    </#list>
}

any|error _reserved = ();

public function main() {
    statements();

    any|error expr = ();
    <#if lastExpr.second>
       ${lastExpr.first}
    <#else>
       expr = trap (${lastExpr.first});
    </#if>

    if (expr is ()){
    } else if (expr is error){
        _io:println("Exception occurred: ", expr.message());
    } else {
        _io:println(expr);
    }
}
