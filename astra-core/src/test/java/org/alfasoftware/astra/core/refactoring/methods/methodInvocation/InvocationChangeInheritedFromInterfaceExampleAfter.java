package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeInheritedFromInterfaceExampleAfter {

  public void example() {
    A a = new A();
    a.getClass();
  }
}

