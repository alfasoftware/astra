package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;

public class InvocationChangeWithAnotherUseOfImportAfter {

  @SuppressWarnings("unused")
  private void example() {
    B.staticOne();
    A.staticTwo();
  }
}

