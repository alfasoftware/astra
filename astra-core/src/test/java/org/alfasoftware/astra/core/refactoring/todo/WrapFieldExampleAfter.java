package org.alfasoftware.astra.core.refactoring.todo;

import java.util.Optional;

import org.alfasoftware.astra.exampleTypes.AnnotationA;
import org.alfasoftware.astra.exampleTypes.a.A;

public class WrapFieldExampleAfter {

  private final Optional<A> a;

  public WrapFieldExampleAfter(
      @AnnotationA Optional<A> a
  ) {
    this.a = a;
  }

  void doSomething() {
    a.get().notify();
  }
}

