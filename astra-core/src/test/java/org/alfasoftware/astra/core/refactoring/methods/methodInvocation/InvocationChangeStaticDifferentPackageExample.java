package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.a.A.staticOne;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeStaticDifferentPackageExample {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    staticOne();
  }
}

