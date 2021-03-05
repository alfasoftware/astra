![Astra logo](images/AlfaAstra-01.png)

## What is Astra?
**Astra** is a tool for analysing and refactoring Java source code, using Java language constructs. 

For example:

* "_**Change** callers of method A to instead call method B_"
* "_**Find** classes which are annotated with C_"

Astra has been developed and tested at Alfa to improve the velocity at which large scale refactors may be performed.

## How do I use Astra?
There are different ways to use Astra, depending on your needs. 

* To perform straightforward refactors, Astra can be run as a Java application, using `astra-core` as a dependency and using the full set of existing operations. For an illustration of how to do this, please see [astra-example](./astra-example/README.md).
* For cases needing a more bespoke approach, [astra-core](./astra-core/README.md) provides an SPI for writing your own operations, implementing `ASTOperation`s to use in your own `UseCase`. See the `astra-core` README for further details.
* For very simple cases, there is a command line interface which exposes a small subset of Astra's refactoring operations. Please see [astra-cli](./astra-cli/README.md) for more information.

## Why would I use Astra?

A simple and common use case is renaming a method, and updating all the callers of that method so that they use the new name. 
Your IDE is normally the best tool for the job, but if the method is used very widely, this may not be possible. 
This means that sometimes it's easier to just add a new method, deprecate the old one, and leave all the existing callers. 
The same need would also apply to renaming a type. Astra can be used to make changes like these easily, and on a massive scale.

## How does Astra work?
Astra compiles source code into an [AST](https://en.wikipedia.org/wiki/Abstract_syntax_tree), a construct that gives the tool useful information about the structure of the code. Astra then allows you to analyse or refactor your source code based on information from the AST.

Here's a sequence diagram explaining how it works:

![Astra logo](images/astraSequence.png)

`AstraCore` iterates through all the Java source files in the input directory. 

File-by-file, Astra will compile an AST - built from the source file, plus any additional classpaths supplied. 
Each individual construct, such as a name, type, expression, statement, or declaration, is represented in the AST by an `ASTNode`.
It also builds an `ASTRewriter`, to record any changes to be made to the AST. 

Astra then visits every `ASTNode` in the AST, passing every node through a set of `ASTOperations`. If an operation is applicable to that node, (e.g. is this an invocation of method A?) the `ASTOperation` records changes in the `ASTRewriter` (e.g. invoke method B instead)

When all `ASTNodes` in a source file's AST have been visited, any changes recorded in the `ASTRewriter` are written back to the original source file. At this stage, Astra will also organise imports in a similar way to an IDE, removing duplicates and unused imports, and sorting.

## Technologies
* Java 8
* Eclipse JDT
* JUnit
* log4j
* Mockito
