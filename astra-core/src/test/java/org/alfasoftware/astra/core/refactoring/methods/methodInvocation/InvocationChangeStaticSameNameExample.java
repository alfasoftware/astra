package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.A.staticOne;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

public class InvocationChangeStaticSameNameExample {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    B.staticOne();
    staticOne();
  }
}

