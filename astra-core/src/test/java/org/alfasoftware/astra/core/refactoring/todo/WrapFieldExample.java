package org.alfasoftware.astra.core.refactoring.todo;

import org.alfasoftware.astra.exampleTypes.AnnotationA;
import org.alfasoftware.astra.exampleTypes.a.A;

public class WrapFieldExample {

  private final A a;

  public WrapFieldExample(
      @AnnotationA A a
  ) {
    this.a = a;
  }

  void doSomething() {
    a.notify();
  }
}
