package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeNewParameterStringLastAfter {

  public void a() {
    A a = new A();
    a.overloaded("", "NEW");
  }
}

