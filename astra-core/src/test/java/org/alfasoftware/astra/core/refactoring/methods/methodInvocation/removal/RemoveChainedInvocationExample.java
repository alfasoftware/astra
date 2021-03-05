package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.exampleTypes.ExampleWithFluentBuilder;

@SuppressWarnings("unused")
public class RemoveChainedInvocationExample {

  private static final HashSet<String> SET_FOR_B = new HashSet<>(Arrays.asList("a string", "another string"));

  /**
   * The three different overloaded methods with a variety of realistic testing scenarios.
   */
  void method() {

    ExampleWithFluentBuilder.withCode("").withA().withB("a string").withC().build();

    ExampleWithFluentBuilder.withCode("")
    .withA()
    .withBs("varargs", "of", "strings")
    .withC()
    .build();

    new ExampleWithFluentBuilder.AcceptorOfClass(
    ExampleWithFluentBuilder.withCode("").withA()
    .withBs(new HashSet<>(Arrays.asList("a string", "another string", "wow hi Joe"))).withC().build()
    );

    ExampleWithFluentBuilder.withCode("").withA().withBs(SET_FOR_B).withC().build();

  }

  // here to clear the "unused" warning
  private void foo() {
  }
}