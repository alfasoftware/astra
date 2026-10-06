# Astra Maven Plugin

# Goal - refactor

## Description

The purpose of this plugin is to run an Astra UseCase over the Maven module(s) source code.

## Plugin Attributes

Does not bind by default to any lifecycle phase - it can be run as a standalone goal or must be attached explicitly, e.g. to the `process-sources` phase for a multi-module build.

## Required Parameters

| Name | Type  | Property | Description |
| -----|-------|----------|-------------|
| usecase | String | astra.usecase | The refactoring UseCase to run. Must be a fully qualified class name on the class path either of the project or the plugin. |

## Optional Parameters

| Name | Type  | Property | Description | Default |
| -----|-------|----------|-------------|---------|
| skip | boolean | astra.skip | Skips execution of the goal | false |
| sourceDirectory | File | sourceDirectory | The source directory to be processed by the refactor | The project base directory. |
| targetDirectory | String | targetDirectory | The target directory for the project | The project build directory. |


## Usage

To use in multi-module projects bind to the `process-sources` phase.

```
[...]

<plugin>
  <groupId>org.alfasoftware</groupId>
  <artifactId>astra-maven-plugin</artifactId>
  <version>...</version>
  
  <!-- optional dependency to provide use-cases if stored separately -->
  <dependency>
    <groupId>org.alfasoftware</groupId>
    <artifactId>astra-example</artifactId>
    <version>...</version>
  </dependency>  
</plugin>

[...]
```

Command line usage once the plugin is defined in the pom just needs to specify the use case:
`mvn astra:refactor -Dastra.usecase=org.alfasoftware.astra.example.ExampleUseCase`

## Running a `JavaPatternASTOperation` (refactor-by-example) through this plugin

`astra-core` provides `JavaPatternASTOperation`, an `ASTOperation` that matches and rewrites code based on a "before" and "after" example written as annotated Java (see the `@JavaPattern`/`@JavaPatternReplacement` annotations in `org.alfasoftware.astra.core.refactoring.operations.javapattern`), rather than on operation-specific builder code. It is only exposed via the `astra-core` SPI, so it is run through this plugin the same way as any other custom `ASTOperation`: wrap it in a `UseCase` and point `astra.usecase` at that class. No plugin code changes are required.

```java
public class MyJavaPatternUseCase implements UseCase {

  @Override
  public Set<? extends ASTOperation> getOperations() {
    try {
      return Set.of(new JavaPatternASTOperation(Path.of("src/main/astra/MyMatcher.java")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
```

`MyMatcher.java` is the matcher file: a small Java file (not necessarily compiled/on any runtime classpath other than at parse time) with one method annotated `@JavaPattern` describing the code to find, and exactly one method annotated `@JavaPatternReplacement` describing what to replace it with. See `astra-core`'s tests under `astra-core/src/test/java/org/alfasoftware/astra/core/refactoring/javapattern/` for worked examples.

Package `MyJavaPatternUseCase` the same way as any other use case (its own module, or a dependency added to the plugin as shown above), then run:
`mvn astra:refactor -Dastra.usecase=com.example.MyJavaPatternUseCase`

