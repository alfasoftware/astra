package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.A.staticOne;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeStaticExample {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    org.alfasoftware.astra.exampleTypes.A.staticOne();
    staticOne();
  }
}
