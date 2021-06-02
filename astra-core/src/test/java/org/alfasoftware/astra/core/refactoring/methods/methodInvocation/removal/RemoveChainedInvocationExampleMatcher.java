package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPattern;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternReplacement;
import org.alfasoftware.astra.exampleTypes.ExampleWithFluentBuilder;

class RemoveChainedInvocationExampleMatcher {

  @JavaPattern
  void pattern(ExampleWithFluentBuilder.Builder builder){
    builder.withB("a string");
  }

  @JavaPattern
  void pattern(ExampleWithFluentBuilder.Builder builder, String[] stringVarargs){
    builder.withBs(stringVarargs);
  }

  @JavaPatternReplacement
  ExampleWithFluentBuilder.Builder patternReplacement(ExampleWithFluentBuilder.Builder builder){
    return builder;
  }
}
