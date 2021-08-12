package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

class InvocationChainedLargeChainsExample {

  @SuppressWarnings("unused")
  private void a() {
    A foo = new A();
    foo.getA().getB().getC().getD();
    foo.getC().getB().getA();
  }
}

