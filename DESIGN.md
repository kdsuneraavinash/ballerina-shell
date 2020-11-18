# Ballerina Shell Design Document

## Preprocessor

Preprocessor is the first transformational phase of the program. Any input is sent through the preprocessor to convert the input into a list of individually processable statements. For an example any multiple statement input will be split into the relevant list of string counterpart at the end of this phase. The implemented `SeparatorPreprocessor` currently splits the statements into separated lists depending on the semicolons that are in th root bracket level. The motivation of a preprocessor is to divide the input into separately identifiable sections so each can be individually processed on.

```java
public interface Preprocessor {
    List<String> preprocess(String input);
}
```

Currently following preprocessors are implemented.

| Preprocessor Name      | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
| Separator preprocessor | Preprocessor to split the input into several statements based on the semicolons and brackets. |
| Combined preprocessor  | Combines several preprocessors into a single preprocessor.   |

Following are some inputs and expected output of the preprocessor for reference.

| Input                                             | Expected Output                                     |
| ------------------------------------------------- | --------------------------------------------------- |
| `int number`                                      | [`int number`]                                      |
| `int number; number = 100;`                       | [`int number;`, `number = 100;`]                    |
| `function () { int p = 100; string h = "hello";}` | [`function () { int p = 100; string h = "hello";}`] |
| `int a = 0; while (a < 10) { a+= 1; }`            | [`int a = 0;`, `while (a < 10) { a+= 1; }`]         |

## Tree Parser

In this stage the correct syntax tree is identified.  The root node of the syntax tree must be the corresponding type for the statement. For an example, for a import declaration, the tree that is parsed should have `ImportDeclarationNode` as the root node.

```java
public interface TreeParser {
    Node parse(String source);
}
```

Currently following tree parsers are implemented.

| Tree Parser Name  | Description                                                  |
| ----------------- | ------------------------------------------------------------ |
| Trial Tree Parser | Parses the source code line using a trial based method. The source code is placed in several places and is attempted to parse. This continues until the correct type can be determined. |

Following are some inputs and expected output of the tree parser for reference.

| Input                       | Expected Output Root Node       |
| --------------------------- | ------------------------------- |
| `import ballerina/io;`      | `ImportDeclarationNode`         |
| `int variable = 100;`       | `ModuleVariableDeclarationNode` |
| `while (a) { int i = 100;}` | `WhileStatementNode`            |

## Snippet Conversion

Snippets are individual statements.

Every snippet must have a **kind** (which dictates where the snippet should go) and a **sub kind** (depicting the statement type) Each snippet must refer to a single statement. That means if the same input line contained several statements, it would be parsed into several snippets. (This separation is done in preprocessor.)

In processing the snippets, if a snippet contained an error and failed to run, the execution of the snippet would be stopped. If the snippet was contained in a line with more snippets, (if the input contained multiple snippets) all the snippets would be ditched. This also means that an error snippet is taken as if it were never given. 

Also, names given to the REPL may never be overridden. (If `x` variable is defined, you cannot redefine variable `x` even with the same type. Same goes for functions, classes etc..) However, any valid redeclaration in a different scope may be possible.

### Snippet Base Type

Snippets are defined in terms of the source code in contains.

```java
public abstract class Snippet {
    protected final SnippetSubKind subKind;
    protected final String sourceCode;
}
```

Snippets would be of mainly 6 categories. Of those 5 categories, erroneous snippet are considered an error and is rejected.

#### Import Snippet

Snippets that represent a import statement. 

Every user import must be done with a prefix. However, REPL would include many base imports by default so that the user does not have to import every module.  Available imports can be viewed via `\imports` internal command.

```bash
=$ /imports
Following standard modules are available without importing.
Any other import needs to be imported with a prefix.
|   crypto, encoding, io, java, java.arrays (as jarrays),
|   jsonutils, jwt, math, reflect, runtime, stringutils,
|   system, time, xmlutils, xslt
```

```java
public class ImportSnippet extends Snippet {
    private final String importPrefix;
}
```

##### Limitations

- Due to limitations in the compiler, practically no imports can be done. According to the language specification one cannot have unused imports which results in import snippets always causing an error. As a result no other imports than the ones that are already there cannot be used.

- All the imports must be done via a prefix. Which means they should follow the following format.

  ```ballerina
   import [org-name /] module-name [version sem-ver] as import-prefix;
  ```

#### Variable Declaration Snippet

Bulk of the handling is done for these type of snippets since these snippet hold the whole REPL state. (as global variables) 

##### Converting to a Module Level Declaration

In the REPL, only module level variables are allowed. The main motivation of that is to keep a global state. (So every defined variable is available to others.) Thus, `VariableDeclarationNodes` will also be converted into `ModuleVariableDeclarationNode`. However, the ability of variable declarations to not have a initializer will be an issue.

##### Infer a filler value is possible

Because every declaration is a module level declaration, they must have a initializer. However, for usability, some variables should have a default value to initialize if the initializer is not given. For an example, an integer can be initialized with a `0` if a initializer is not provided. However, this will only be done for selected few types where a default initializer is trivial.

Following initializers will be used when a initializer is not provided. Note that, because lack of information at the stage of this operations, it is not possible to infer the default types of the types which are defined by the user. This will also include `var`, which is a type determined by the compiler. The table also include the whether the type is serializable.

| Type                                                         | Filler Default Value                          | Serializable                            |
| ------------------------------------------------------------ | --------------------------------------------- | --------------------------------------- |
| `()`                                                         | `()`                                          | Yes                                     |
| `boolean`                                                    | `false`                                       | Yes                                     |
| `int`, `float`, `double`, `byte`                             | `0`                                           | Yes                                     |
| `string`                                                     | `""`                                          | Yes                                     |
| `xml`                                                        | `xml '<!---->'`                               | Yes                                     |
| `array`, `tuple`                                             | `[]`                                          | if all contained types are serializable |
| `map`                                                        | `{}`                                          | if all contained types are serializable |
| `record`                                                     | `{}`                                          | No                                      |
| `table`                                                      | `table []`                                    | No                                      |
| `any` (not supported)                                        | `()`                                          | No                                      |
| `union`                                                      | any available filler value of types in union. | if all contained types are serializable |
| `optional`                                                   | `()`                                          | if the optional type is serializable    |
| `json`, `anydata`                                            | `()`                                          | Yes                                     |
| `intersection`, `singleton`, `readonly`, `never`, `distinct` | None                                          | No                                      |
| all behavioral types                                         | None                                          | No                                      |
| all other types                                              | None                                          | No                                      |

*Default values for `xml`, `array`, `tuple`, `map`, `record` have the potential to fail or cause errors. However, they are still used if the user didn't provide one in the expectation that user will identify the error if the default value failed. [Ballerina Default Fill Members](https://ballerina.io/spec/lang/2020R1/#FillMember)

Following are some inputs and expected filler values for reference.

| Input                     | Expected Type | Expected Filler Value(s) |
| ------------------------- | ------------- | ------------------------ |
| `int number;`             | `int`         | `0`                      |
| `error|int|string value;` | `union`       | `0` or `""`              |
| `var t`                   | `other`       | None                     |

> TODO: How to create a empty stream? Should we try to initialize objects with new T()? How to use singletons?

##### Identifying Variable Name

For the serialization (which enables state preservation), variable names has to be identified. Currently the name from the binding is taken as the name. (Note: Only capturing binding patterns are accepted.)

##### Limitations

- `any` type is not supported. `any` does not support `==` operator. As a result generating code for any type would require custom handling. Thus they are disabled for the moment.
- Current logic only supports `CaptureBindingPatternNode` as the binding pattern. 
  - `WildcardBindingPatternNode` and `ErrorBindingPatternNode` are not valid binding patterns for global variables. (They do not declare new variable names) `MappingBindingPatternNode` and `ListBindingPatternNode` crashes the compiler when defined at the global level. Thus these are rejected once identified. `NamedArgBindingPatternNode` and `RestBindingPatternNode` are not possible since no other binding patterns are available.
- Variable names must not start with `_`. Variables starting with `_` are reserved by the code generator.

#### Module Member Declaration Snippet

Module level declarations. These are not active or runnable. Service declaration have the ability to start a service on a port, etc...  All other declarations are just declarations. They do not execute to return a value. Also, any undefined variable in these declarations are ignored. These do not contain semicolons at the end. 

##### Sub Kinds

| Sub Kind Name                    | State    | Notes                                                        |
| -------------------------------- | -------- | ------------------------------------------------------------ |
| Function Definition              | OK       |                                                              |
| Listener Declaration             | OK       | There must be a initializer. However, there may be undefined variables inside the initializer. |
| Type Definition                  | OK       |                                                              |
| Service Declaration              | REJECTED | Has a side effect of starting a server.                      |
| Constant Declaration             | OK       | Constant variables are always defined in the module level.   |
| Module Variable Declaration      | MOVED    | Moved responsibility into Variable Declaration Kind.         |
| Annotation Declaration           | OK       | TODO: No examples found.                                     |
| Module XML Namespace Declaration | OK       |                                                              |
| Enum Declaration                 | OK       |                                                              |
| Class Definition                 | OK       |                                                              |

##### Examples

```C#
function printValue(string value) { } // Function Definition
listener http:Listener helloWorldEP = new (9095, helloWorldEPConfig); // Listener Declaration
type newType record{string name;}; // Type Definition
service hello on new http:Listener(9090) { } // Service Declaration 
const int var1 = 3; // Constant Declaration
// TODO: No examples // Annotation Declaration
xmlns "http://ballerina.com/aa" as ns0; // Module XML Namespace Declaration
enum Color { RED, GREEN, BLUE } // Enum Declaration
class Person { } // Class Definition
```

#### Statement Kind

These are normal statements that should be evaluated from top to bottom inside a function. Fail Statement Sub Kind are not accepted. 

##### Sub Kinds

| Sub Kind Name                       | State | Notes                                                        |
| ----------------------------------- | ----- | ------------------------------------------------------------ |
| Assignment Statement                | OK    |                                                              |
| Compound Assignment Statement       | OK    |                                                              |
| Variable Declaration Statement      | MOVED | Moved responsibility into Variable Declaration Kind.         |
| Block Statement                     | OK    |                                                              |
| Break Statement                     | ERROR | Break cannot be used outside of a loop.                      |
| Fail Statement                      | ERROR | Fail statements must appear inside a function. (Similar to return) |
| Expression Statement                | MOVED | Moved responsibility into Expression Kind.                   |
| Continue Statement                  | ERROR | Continue cannot be used outside of a loop.                   |
| If Else Statement                   | OK    |                                                              |
| While Statement                     | OK    |                                                              |
| Panic Statement                     | OK    | Similar to throwing an error. Will throw the error and ignore from then. |
| Return Statement                    | ERROR | Return cannot exist outside of a function.                   |
| Local Type Definition Statement     | MOVED | Moved responsibility into Module Member Declaration Kind.    |
| Lock Statement                      | OK    | Atomically change the values of the variables.               |
| Fork Statement                      | OK    | Starts workers. (Might cause problems)                       |
| For Each Statement                  | OK    |                                                              |
| XML Namespace Declaration Statement | MOVED | Moved responsibility into Module Member Declaration Kind.    |
| Transaction Statement               | OK    |                                                              |
| Rollback Statement                  | ERROR | Rollback cannot be used outside of a transaction block.      |
| Retry Statement                     | OK    | Retry can exist outside transactions as a general purpose control. |
| Match Statement                     | OK    | Similar to switch statements.                                |
| Do Statement                        | OK    | TODO: No examples found.                                     |

##### Examples

```C#
var1 = 3; // Assignment Statement
var1 += 3; // Compound Assignment Statement
{ int var1 =4; } // Block Statement
if (cond) { } // If Else Statement
while (cond) { } // While Statement
panic error("Record is nil"); // Panic
lock { amount += n; } // Lock
fork { } // Fork
foreach var color in colors { } // For Each Statement
transaction { } // Transaction Statement
retry<Type> (args) { } // Retry Statement
match var1 { 0 => {} 1 => { } } // Match Statement
// TODO: No examples // Do Statement
```

#### Expression Kind

These are expressions that are executable but are not persistent. (Does not affect other statements/expressions) These do not contain semicolons. (If the expression is a Expression Statement, or a statement with a semicolon, the semicolon will be stripped.)

##### Examples

```C#
var1 == var2 // Binary Expression
(var1 == var2) // Braced Expression
check func1(10) // Check Expression
abc.value // Field Access Expression
func1(arg1) // Function Call Expression
abs.method(arg1)// Method Call Expression
{ line: "str", country: "abc" } // Mapping Constructor Expression
typeof var1 // Typeof Expression
! var1 // Unary Expression 
object { public string city; } // Object constructor Expression
var1 is Type // Type Test Expression [ISSUE]
abc->method(arg1) // Action
() // Nil Literal
4 // Basic Literal
int|string // Type Descriptor [ISSUE] : Abstract so cant detect
trap func1(10) // Trap Expression
[1, 2, 3, 4] // List Constructor Expression
<float> v1 // Type Cast Expression
table [ {id: 1, name: "J"}, {id: 2, name: "B"} ] // Table Constructor Expression [ISSUE]
let int x = 2 in x*2 // Let Expression
string `INSERT INTO Details VALUES (${p.name}, ${p.age})` // Template Expression
function(int arg1) { } // Annonymous Function Expression
new Abc(arg1) // New Expression
from var student in studentList select { name: student.firstName }   // Query Expression
start func1(arg1) // Start Action
flush worker1 // Flush Action [ISSUE]
// TODO: No examples // Annot Access Expression
abc?.value // Optional Field Access Expression
cond ? func1() : func2() // Conditionl Expression
transactional // Transactional Expression
@http:WebSocketServiceConfig {} service {} // Service Constructor Expression ((DECIDE))
base16 `112233` // Byte Array Expression
bookXML/**/<fname> // XML Navigate Expression
```

#### Erroneous Kind

A syntactically incorrect input for which the specific kind could not be determined.

## Implementations

### Naive Implementation

Initial implementation of the ballerina shell. Each cycle would run similar to the below pipeline.

Here following wrapper will be used to wrap the given source code parts.

```C#
import ballerina/io;
// <= IMPORT LEVEL STUB

// <= MODULE LEVEL STUB

function stmts() returns error? {
    // <= STATEMENT STUB
}

function do_it() returns string|error{
    check stmts();
    any|error expr = // <= EXPRESSION WITH SEMICOLON STUB
    any value = checkpanic expr;
    string repr = io:sprintf("%s", value);
    return repr;
}

public function main(){
    string|error result = trap do_it();
    if (result is string) {
        io:println(result);
    } else {
         io:println("Error occurred: ", result.message());
    }
}
```





---

## Issues

#### Importing may result in an error

```python
>> import bals/io;
unused import module 'ballerina/io'
```

#### REPL evaluation of truth-y/false-y expressions result in an error

```python
>> int b = 3;
>> b is int
expression will always evaluate to 'true'
```

#### Can't evaluate type descriptors in REPL

```python
>> int|float
operator '|' not defined for 'typedesc<int>' and 'typedesc<float>'
```

#### Can't parse table expression into any|error type

```python
>> table [ {id: 1, name: "J"}, {id: 2, name: "B"} ]
incompatible types: expected '(any|error)', found 'table<record {| int id; string name; |}>'
```
