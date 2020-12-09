# Ballerina Shell

A REPL program for the [ballerina language](https://github.com/ballerina-platform/ballerina-lang).  Ballerina is an open source programming language and platform for  cloud-era application programmers to easily write software that just works.

The Ballerina-shell tool is an interactive tool for learning the Ballerina programming language and prototyping Ballerina code. Ballerina-shell is a Read-Evaluate-Print Loop (REPL), which evaluates declarations, statements, and expressions as they are entered and immediately shows the results. Currently, the tool is run via the command line. Using Ballerina-shell, you can enter program statements one at a time and immediately see the result.

## Demo

![Recording](./docs/demo.gif)

## Modules

The project is implemented in two base modules.

- **shell** - Module including all the base evaluation classes. This has all the base components to evaluate and run a string. All other components are built on top of this module. You may find the source code for this module [here](shell).
- **shell-cli** - A command-line interface built on top of shell. Includes multi-line inputs, color-coded outputs, keyword-based auto-completion, etc... You may find the source code for this module [here](shell-cli).

## Known Issues

- **The parser is imperfect** - Current parser is imperfect and is sometimes unable to detect the type of statement. Please file an issue if you come across any wrong categorization of a snippet. The parser is also relatively slow compared to the compilation phase, acting as a bottle-neck. So a timeout is employed to stop invalid statement parsing from taking too much time. However, this might cause issues in some old hardware where the execution might be slower than expected (where even valid executions might exceed the timeout).

- **Assignments to global variables in closures or class methods will not work** - Assignments done to global variables in closures will not be reflected after the execution. The changes will be visible only for the scope belonging to the snippet where the closure was defined.

  ```ballerina
  int x = 10
  var f = function () { x = 12; }
  f()
  x   // <- this should output 12 but will output 10 instead
  ```

- **Type guards for global variables do not work** - Since all the variables defined in REPL top level act as global variables, type guards won't work. To remedy this you can explicitly cast the variable to the required type.

  ```ballerina
  string|int x = "hello"
  
  // Following will not work
  if (x is string) { io:println(x.length()); }
  // Use following instead
  string x_str = <string> x;
  io:println(x_str.length());
  ```

- **Only captured binding patterns are supported to define variables** - You may not use list binding pattern/mapping binding pattern to define global variables in REPL. (Local variables can still be defined using these)

  ```ballerina
  // This will not work
  var [a, b] = [1, 2]
  // Use following instead
  int a = 0; int b = 0;
  [a, b] = [1, 2]
  ```

- **When using query expressions, use them only as assignments** - If a query expression is done as a variable declaration, it would throw an exception. Input the expression as an assignment instead. (Declare the variables first)

  ```ballerina
  // This will not work
  string x = from var y in z where ....
  // Use following instead
  string x = ""
  x = from var y in z where ....
  ```

## Implementation

For implementation details please refer [this](shell/README.md).

## Building

> **Linux** - Simply clone the repository and run `run.sh`. It should launch the REPL.

Run following commands in order.

```batch
gradlew.bat fatJar
java -jar -Dballerina.home=home shell-cli/build/libs/shell-cli-1.0-SNAPSHOT.jar
```

**To run with an installed ballerina distribution,** (This will enable stdlib imports)
In the following script, `$BALLERINA_HOME` refers to the ballerina distribution directory.
Ballerina shell is compatible with `ballerina-slp7` or higher.

```bash
java -jar -Dballerina.home=$BALLERINA_HOME shell-cli/build/libs/shell-cli-1.0-SNAPSHOT.jar
# eg: java -jar -Dballerina.home=/usr/lib/ballerina/distributions/ballerina-slp7 shell-cli/build/libs/shell-cli-1.0-SNAPSHOT.jar
```

##  References

[reple: "Replay-Based" REPLs for Compiled Languages](https://people.eecs.berkeley.edu/~brock/blog/reple.php) - A blog post on reple: "Replay-Based" REPLs for Compiled Languages and limitations/fixes possible.

[RCRL](https://github.com/onqtam/rcrl) - Read-Compile-Run-Loop: tiny and powerful interactive C++ compiler (REPL)

[JShell](https://docs.oracle.com/javase/9/jshell/introduction-jshell.htm#JSHEL-GUID-630F27C8-1195-4989-9F6B-2C51D46F52C8) - A REPL for Java programming language.

