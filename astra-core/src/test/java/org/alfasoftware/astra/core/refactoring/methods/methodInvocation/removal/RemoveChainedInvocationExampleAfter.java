package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.exampleTypes.ExampleWithFluentBuilder;

@SuppressWarnings("unused")
public class RemoveChainedInvocationExampleAfter {

  private static final HashSet<String> SET_FOR_B = new HashSet<>(Arrays.asList("a string", "another string"));

  /**
   * The three different overloaded methods with a variety of realistic testing scenarios.
   */
  void method() {

    ExampleWithFluentBuilder.withCode("").withA().withC().build();

    ExampleWithFluentBuilder.withCode("").withA()
    .withC()
    .build();

    new ExampleWithFluentBuilder.AcceptorOfClass(
    ExampleWithFluentBuilder.withCode("").withA().withC().build()
    );

    ExampleWithFluentBuilder.withCode("").withA().withC().build();

  }

  // here to clear the "unused" warning
  private void foo() {
  }
}