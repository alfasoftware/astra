![Astra logo](images/AlfaAstra-01.png)

## What is Astra?
**Astra** is a tool for analysing and refactoring Java source code, using Java language constructs. 

For example:

* "_References to type A should instead reference type B_"
* "_Callers of method A should add an additional argument B_"
* "_Find classes which are annotated with A_"

Astra has been developed and tested at Alfa to improve the velocity at which large scale refactors may be performed.

## How do I use Astra?
* First, please see [the Wiki!](https://github.com/alfasoftware/astra/wiki)
* Astra can be run as part of a Java application, using `astra-core` as a dependency and using the refactors it provides. For an illustration of how to do this, please see the [README in astra-core](./astra-core/README.md). The code can be found in [astra-example](./astra-example).
* For cases needing a more bespoke approach, [astra-core](./astra-core/README.md) also provides an SPI for writing your own custom `ASTOperation`s. See the `astra-core` README for further details.
* For very simple cases, there is also a command line interface which exposes a small subset of Astra's refactoring operations. Please see [astra-cli](./astra-cli/README.md) for more information.

## Why would I use Astra?
A simple and common use case is renaming a method, and updating all the callers of that method so that they use the new name. 
Your IDE is often the best tool for the job, but sometimes this isn't possible. There may be so many modules that manually selecting and opening them is a real pain, or the overall size of the modules may mean that your IDE struggles to open them all at once. 
This means that sometimes it's easier to just add a new method, deprecate the old one, and leave all the existing callers. 
The same issues apply to many other refactors, such as renaming a type. 

Astra can be used to make changes like these easily, and on a massive scale.

## How does Astra work?
Please see [How does Astra work?](https://github.com/alfasoftware/astra/wiki/How-does-Astra-work%3F) in the Wiki.

## Technologies
* Java 8
* Eclipse JDT
* JUnit
* log4j
