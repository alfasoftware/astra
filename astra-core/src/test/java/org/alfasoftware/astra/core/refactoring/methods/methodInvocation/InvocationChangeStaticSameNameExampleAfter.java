package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.C.staticTwo;

import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.C;

public class InvocationChangeStaticSameNameExampleAfter {

  @SuppressWarnings("unused")
  private void example() {
    C.staticTwo();
    B.staticOne();
    staticTwo();
  }
}

