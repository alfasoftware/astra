package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;

public class InvocationChangeWithAnotherUseOfImport {

  @SuppressWarnings("unused")
  private void example() {
    A.staticOne();
    A.staticTwo();
  }
}

