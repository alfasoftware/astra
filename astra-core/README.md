# Writing your first refactor
## Decide on a change
Before writing a refactor, we need to decide what we are trying to achieve. Below is an example refactor. The code can be found in the `astra-example` module.

Let's say we have an interface in our code base, `FooBarInterface`:
```java
public interface FooBarInterface {

  @Deprecated
  void doFoo();

  void doBar();
}
```
It has two methods, `doFoo` and `doBar`. The `doFoo` method has been deprecated and a new preferred method named `doBar` has been added.

Here is an example caller of the `FooBarInterface`.
```java
public class FooBarCaller {
 
  private FooBarInterface fooBarInterface;
 
  FooBarCaller(FooBarInterface fooBarInterface) {
    this.fooBarInterface = fooBarInterface;
  }
 
  void doThing() {
    fooBarInterface.doFoo();
  }
}
```
We want to update all the existing callers to instead call the `doBar` method instead.

## Writing the Astra `UseCase`
The inputs to `AstraCore` are bundled up in a `UseCase`. This contains:

* A set of `ASTOperations` - visitors for `ASTNodes` which specify analysis or refactoring tasks,
* Any additional _classpaths_ needed for building a detailed AST.

You can write a new `UseCase` using existing general-purpose `ASTOperations`, like the `MethodInvocationRefactor`. Or for something more specialised, you may want to define a new type of `ASTOperation`.


```java
public class FooBarUseCase implements UseCase {
   
  @Override
  public Set<? extends ASTOperation> getOperations() {
    return Sets.newHashSet(
        MethodInvocationRefactor
          .from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType("org.alfasoftware.astra.example.target.FooBarInterface")
              .withMethodName("doFoo")
              .build())
          .to(
              new MethodInvocationRefactor.Changes().toNewMethodName("doBar")
          )
    );
  }
  
  @Override
  public Set<String> getAdditionalClassPathEntries() {
    return new HashSet<>(Arrays.asList(
      "C:\Users\Me\.m2\repository\com\example\1.0-SNAPSHOT\foobar-api-1.0-SNAPSHOT.jar",
      "C:\Users\Me\.m2\repository\com\example\1.0-SNAPSHOT\foobar-impl-1.0-SNAPSHOT.jar"
    ));
  }
}
```
Here, we also supply the classpath to the jar files containing the `FooBarInterface` and `FooBarClass`, by overriding `UseCase.getAdditionalClassPathEntries()`.
To illustrate this example, we could imagine that our interface is in `foobar-api`, and the class in `foobar-impl`, so we supply these as absolute paths to local jar files. The example shows paths to a local maven repository. 
These classpaths help Astra to interpret our source code. In this case they allow Astra to see that `FooBarClass` implements `FooBarInterface`.

## Applying the UseCase
To apply the `UseCase`, we need to use it as an argument to `AstraCore.run()`. This method accepts 2 arguments:

* The `directory` to apply the `UseCase` over.
* The `UseCase` to apply.

```java
public class AstraRunner {
 
  public static void main(String[] args) {
    AstraCore.run(
      directoryPath,
      useCase);
  }
}
```
And when we look at our calling code again, we can see that `doFoo` has been changed to `doBar`.
```java
public class FooBarCaller {
 
  private FooBarInterface fooBarInterface;
 
  FooBarUser(FooBarInterface fooBarInterface) {
    this.fooBarInterface = fooBarInterface;
  }
 
  void doThing() {
    fooBarInterface.doBar();
  }
}
```
Congratulations, you've completed your first Astra refactor! There's a lot more that Astra can do - check out all the other subtypes of `ASTOperation` to see other refactoring and code analysis operations.

# Skipping files that can't be changed
Parsing a source file, with the bindings Astra needs to understand it, is usually the most expensive part of a run. A file that none of the operations can change doesn't need to be parsed at all, so Astra tests each file's raw content first, and only parses the files that pass.

There are two places to say which files can be skipped. A file is only parsed if it passes both.

## `UseCase.getContentPrefilteringPredicate()`
Override this on your `UseCase` to filter files for the whole use case. For example, if only files that mention `FooBarInterface` can need changing:
```java
  @Override
  public Predicate<String> getContentPrefilteringPredicate() {
    return content -> content.contains("FooBarInterface");
  }
```
This runs after `UseCase.getPrefilteringPredicate()`, which filters on the file path. The default accepts every file.

## `ASTOperation.getContentPrefilteringPredicate()`
An operation can also say which files it could apply to. A file is parsed if **at least one** of the use case's operations accepts it, and once a file is parsed it is passed to every operation as usual.

Returning `false` for a file's content is a promise that `run()` would do nothing for any node of that file, so a predicate must never reject a file the operation could change. A plain text search is fine for this, as long as it only looks for text that must be present. The default accepts every file, which is always safe, so existing operations behave as before, and a use case containing an operation that doesn't override this method parses every file.

```java
public class FooToBarOperation implements ASTOperation {

  @Override
  public void run(CompilationUnit compilationUnit, ASTNode node, ASTRewrite rewriter) {
    // ... only ever changes invocations of doFoo()
  }

  @Override
  public Predicate<String> getContentPrefilteringPredicate() {
    // a file which never mentions doFoo can't contain an invocation of it
    return content -> content.contains("doFoo");
  }
}
```

## Java patterns
`JavaPatternASTOperation` provides this predicate itself, so java pattern use cases get it with no extra work. A `@JavaPattern` can only match code which contains the names of the methods it invokes and the types it instantiates, so files without them, for every `@JavaPattern` in the matcher file, are skipped. Names which a pattern captures rather than matches are not required: `@Substitute` methods, the pattern's parameters, type variables, and the arguments captured by a varargs parameter.

This works best when those names are rare in the codebase, as they usually are when migrating away from a particular API. A pattern for something like `string.equals("")` can't skip much, because nearly every file mentions `equals`.
