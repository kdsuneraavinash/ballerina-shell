# Ballerina Shell

A REPL program for the [ballerina language](https://github.com/ballerina-platform/ballerina-lang).  Ballerina is an open source programming language and platform for  cloud-era application programmers to easily write software that just works.

The Ballerina-shell tool is an interactive tool for learning the Ballerina programming language and prototyping Ballerina code. Ballerina-shell is a Read-Evaluate-Print Loop (REPL), which evaluates declarations, statements, and expressions as they are entered and immediately shows the results. Currently, the tool is run via the command line. Using Ballerina-shell, you can enter program statements one at a time and immediately see the result.

## Demo

![Recording](./docs/demo.gif)

## Current Approach

Current approach with the ballerina-shell is a Replay-Based approach.  With this approach, each time the user enters a line of code, a file is generated containing the snippet and the whole program is compiled and executed again/replayed, and any new output is printed. 

Following is the template for the code generation. Once the program is run, any compiler errors/warnings are displayed to the user. A print guard is used so that the user will not see previous print statements. 

```ballerina
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
           _reserved = ${stmt.first};
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
       expr = trap ${lastExpr.first};
    </#if>

    if (expr is ()){
    } else if (expr is error){
        var color_start = "\u{001b}[33;1m";
        var color_end = "\u{001b}[0m";
        _io:println(color_start, "Exception occurred: ", expr.message(), color_end);
    } else {
        _io:println(expr);
    }
}
```

## Modules

The project is implemented in two base modules.

- `shell` - Module including all the base evaluation classes. This merely evaluated strings.
- `shell-cli` - A CLI built on top of `shell`.

## Limitations

Obvious limitation is the **interpretation performance**. Because of this approach, the compilation has to be done in every step. This is significantly slow and affects the user experience. Even if this approach is used by some other projects as well, because of the current efficiency of the ballerina compiler, any statement evaluation takes a considerable amount of time.

Another limitation is **randomness**, because of the replaying approach, any statement that involves randomness will result in different evaluation each time. This will also include any statement that interacts with the external environment such as network calls, user inputs, etc... This can be mitigated by persisting global variable values at the end of the execution. However, since some of the types do not support serialization, this issue can only be mitigated via serializing any value that supports and de-serializing  at the next execution startup just before the evaluation of the new statement.

Another limitation is **re-evaluation** of performance heavy snippets. For an example, a loop containing a significant amount of iteration will cause the execution to be extremely slow because it needs to be evaluated at the each execution. To fix this we can use the serializing approach together with removing any statements of which the state afterwards is correctly preserved. So if the statement did not change the program state (values of global variables) or the changes were correctly persisted, we can remove these statements.

Other limitations include, race conditions involving parallel execution, service declarations not being supported, etc...

In the current implementation due to the integration with the project API, imports are not supported.

## Possible approaches

Other than this, there are few other approaches possible that are used by other similar projects.

> TODO: Add possible approaches

## References

[reple: "Replay-Based" REPLs for Compiled Languages](https://people.eecs.berkeley.edu/~brock/blog/reple.php) - A blog post on reple: "Replay-Based" REPLs for Compiled Languages and limitations/fixes possible.

[JShell](https://docs.oracle.com/javase/9/jshell/introduction-jshell.htm#JSHEL-GUID-630F27C8-1195-4989-9F6B-2C51D46F52C8) - A REPL for Java programming language.

