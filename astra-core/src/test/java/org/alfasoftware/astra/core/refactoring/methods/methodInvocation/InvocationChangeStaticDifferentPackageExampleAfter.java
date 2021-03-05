package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.a.A.staticTwo;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeStaticDifferentPackageExampleAfter {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    staticTwo();
  }
}

