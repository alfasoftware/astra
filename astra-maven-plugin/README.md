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

