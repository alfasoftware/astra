package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

public class InvocationChangeOutsideClassQualifiedExample {

  public void example() {
    A a = new A();
    a.notify();

    B b = new B();
    b.notify();
  }
}
