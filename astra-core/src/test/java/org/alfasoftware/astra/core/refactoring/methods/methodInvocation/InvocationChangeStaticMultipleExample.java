package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.A.staticOne;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeStaticMultipleExample {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    A.staticOne();
    org.alfasoftware.astra.exampleTypes.A.staticOne();
    org.alfasoftware.astra.exampleTypes.A.staticOne();
    staticOne();
    staticOne();
  }
}
