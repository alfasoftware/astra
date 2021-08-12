package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

class InvocationChainedLargeChainsExampleAfter {

  @SuppressWarnings("unused")
  private void a() {
    A foo = new A();
    foo.getD().getC().getB().getA();
    foo.getA().getB().getC();
  }
}

