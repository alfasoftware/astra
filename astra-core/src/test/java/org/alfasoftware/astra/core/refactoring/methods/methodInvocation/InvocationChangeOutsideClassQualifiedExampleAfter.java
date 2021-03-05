package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

public class InvocationChangeOutsideClassQualifiedExampleAfter {

  public void example() {
    A a = new A();
    a.getClass();

    B b = new B();
    b.notify();
  }
}
