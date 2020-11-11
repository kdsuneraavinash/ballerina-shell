# Ballerina Shell Design Document

## Snippets

Every snippet must have a kind (which dictates where the snippet should go and the sub kind depicting the statement type) Each input line in the REPL can refer to one or more snippets. (Separated by semicolons) These will be handled differently. That means even if the same input line contained several statements, it would be taken as if they were separate lines.

In processing the snippets, if a snippet contained an error and failed to run, it would be ditched. Which means that an error snippet is taken as never given. Also, names given to the REPL may never be overridden. (If `x` variable is defined, you cannot redefine variable `x` even with the same type. Same goes for functions, classes etc..)

Currently all `visibilityIdentifiers` will be ignored. Every entry will be public always to make sure of the visibility.

**What to do with `metadata` nodes?**

### Snippet Kinds

#### Import Kind

How to defer unused imports until they are used?

#### Variable Declaration Kind

These will be variable declarations. These statements will be split into declaration and initialization. Declaration would contain the declaration and setting of a default variable. This would happen in Module Level Stub. Initialization would happen in the Statement Stub.

```C#
int a = 4;
```

#### Module Member Declaration Kind

Module level declarations. These are not active or runnable except for service declarations. Service declaration have the ability to start a service on a port, etc...  All other declarations are just declarations. They do not execute to return a value. Also, any undefined variable in these declarations are ignored (Except for module level variable declarations).

##### Sub Kinds

| Sub Kind Name                    | State  | Notes                                                        |
| -------------------------------- | ------ | ------------------------------------------------------------ |
| Function Definition              | OK     |                                                              |
| Listener Declaration             | OK     | There must be a initializer. However, there may be undefined variables inside the initializer. |
| Type Definition                  | OK     |                                                              |
| Service Declaration              | DECIDE | Has a side effect of starting a server.                      |
| Constant Declaration             | OK     | Constant variables are always defined in the module level.   |
| Module Variable Declaration      | MOVED  | Moved responsibility into Variable Declaration Kind.         |
| Annotation Declaration           | DECIDE | TODO: No examples found.                                     |
| Module XML Namespace Declaration | OK     |                                                              |
| Enum Declaration                 | OK     |                                                              |
| Class Definition                 | OK     |                                                              |

##### Examples

```C#
function printValue(string value) { } 									// Function Definition
listener http:Listener helloWorldEP = new (9095, helloWorldEPConfig); 	 // Listener Declaration
type newType record{string name;}; 										// Type Definition
service hello on new http:Listener(9090) { } 							// Service Declaration 
const int var1 = 3; 													// Constant Declaration
// TODO: No examples 													// Annotation Declaration
xmlns "http://ballerina.com/aa" as ns0; 								// Module XML Namespace Declaration
enum Color { RED, GREEN, BLUE } 										// Enum Declaration
class Person { } 														// Class Definition
```

#### Statement Kind

These are normal statements that should be evaluated from top to bottom inside a function. Fail Statement Sub Kind are not accepted. **Can we restore stack frame(state) when new statements has to run, instead of re-evaluating the previous statements?**

In statement kinds, the expression stub will be filled by `()`.

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
var1 = 3; 						// Assignment Statement
var1 += 3; 						// Compound Assignment Statement
{ int var1 =4; } 				// Block Statement
if (cond) { } 					// If Else Statement
while (cond) { } 				// While Statement
panic error("Record is nil"); 	// Panic
lock { amount += n; } 			// Lock
fork { } 						// Fork
foreach var color in colors { } // For Each Statement
transaction { } 				// Transaction Statement
retry<Type> (args) { } 			// Retry Statement
match var1 { 0 => {} 1 => { } } // Match Statement
// TODO: No examples 			// Do Statement
```

#### Expression Kind

These are expressions that are executable but are not persistent. (Does not affect other statements/expressions) These do not contain semicolons. (If the expression is a Expression Statement, or a statement with a semicolon, the semicolon will be stripped.)

##### Examples

```C#
var1 == var2 														// Binary Expression
(var1 == var2) 														// Braced Expression
check func1(10) 													// Check Expression
abc.value 															// Field Access Expression
func1(arg1) 														// Function Call Expression
abs.method(arg1) 													// Method Call Expression
{ line: "str", country: "abc" } 									// Mapping Constructor Expression
typeof var1 														// Typeof Expression
! var1 															  	// Unary Expression 
object { public string city; } 										// Object constructor Expression
var1 is Type 														// Type Test Expression [ISSUE]
abc->method(arg1) 													// Action
() 																	// Nil Literal
4 																	// Basic Literal
int|string 															// Type Descriptor [ISSUE] : Abstract so cant detect
trap func1(10) 														// Trap Expression
[1, 2, 3, 4] 														// List Constructor Expression
<float> v1 															 // Type Cast Expression
table [ {id: 1, name: "J"}, {id: 2, name: "B"} ] 					// Table Constructor Expression [ISSUE]
let int x = 2 in x*2 												// Let Expression
string `INSERT INTO Details VALUES (${p.name}, ${p.age})` 			// Template Expression
function(int arg1) { } 												// Annonymous Function Expression
new Abc(arg1) 														// New Expression
from var student in studentList select { name: student.firstName }   // Query Expression
start func1(arg1) 													// Start Action
flush worker1 														 // Flush Action [ISSUE]
// TODO: No examples 												// Annot Access Expression
abc?.value 															// Optional Field Access Expression
cond ? func1() : func2() 											// Conditionl Expression
transactional 														// Transactional Expression
@http:WebSocketServiceConfig {} service {} 							 // Service Constructor Expression ((DECIDE))
base16 `112233` 													// Byte Array Expression
bookXML/**/<fname> 													// XML Navigate Expression
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
>> import ballerina/io;
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