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