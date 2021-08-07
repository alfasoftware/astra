package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChainedExample {

  @SuppressWarnings("unused")
  private void a() {
    A foo = new A();
    foo.first().second();
  }
}

