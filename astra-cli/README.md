# Astra CLI

## Why would I use this?
A common development task is changing the name of a method, and updating all the places where that method is used. 
Our IDEs are normally the best tool for the job, but if the method is used too widely, and in modules we haven't opened, this may not be possible. 
This means that sometimes it's easier to just deprecate it, like you see below, and leave it in our source code forever. 
```java
/**
 * @deprecated use {@link #getBar()}
 */
@Deprecated 
public String getFoo() {
  return bar;
}
```
The same issue applies when renaming a type. 
Astra can be a good fit for these situations, and could help make development faster. 

There are now two ways to use Astra. You can:
* write your change in Java using `astra-core`'s SPI, following the steps from the README , or
* use this command line runner. 

## Sounds good, let's take it for a spin!
We've initially added support for two common Astra refactors to this CLI. We think they're probably the most widely useful - the `MethodInvocationRefactor` and the `TypeReferenceRefactor`.

| Refactor | Description |
| -------- | ----------- |
| Method invocation refactor | Find uses of method A, and instead, use method B |
| Type reference refactor | Find uses of type A, and instead, use type B |


| Refactor | What's the change? | Example astra-cli invocation |
| -------- | ------------------ | ---------------------------- |
| Method invocation | Update method invocation - where the deprecated `getFoo` is invoked, instead invoke `getBar`. The method is distinguished by the fully qualified declaring type `com.example.Foo`, the method name `getFoo`, and the parameters. Here, `com.example.Foo` is declared in a jar `example-impl-1.0.jar`, in the user's local maven repository. The method name, and an empty parameter list (ie - no parameters) are also specified. | `astra method --fqType com.example.Foo --name getFoo --newname getBar --parameters= --cp C:\Users\ExampleUser\.m2\repository\com\example\example-impl\example-impl-1.0.jar --directory C:/Code/CodeBase` |
| Type reference  |Deduplicating types - swap all uses of the type `com.example.Foo` to `com.example.Bar`. The usage type will be preserved - simple or fully qualified referenced will be replaced in kind. Here, `com.example.Foo` is declared in a jar file example-impl-1.0.jar, in the user's local maven repository. Note that no additional classpaths are required for the change to `Bar`. | `astra changetype com.example.Foo com.example.Bar --cp C:\Users\ExampleUser\.m2\repository\com\example\example-impl\example-impl-1.0.jar -d C:/Code/CodeBase` |


## What are its limitations?
The CLI currently only supports these two refactors, and even then are very simplified versions. The standard astra-core SPI offers far more options for these refactors, like changing the method parameters, offering predicate / pattern matching (like "does the method name start with *get"), and defining custom changes. 

We hope that this bare-bones version of the CLI will help us gauge interest, collect early feedback, and make sure we focus our efforts on the things that provide the most value.
