package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import static org.alfasoftware.astra.exampleTypes.B.staticTwo;

import org.alfasoftware.astra.exampleTypes.B;

public class InvocationChangeStaticMultipleExampleAfter {

  @SuppressWarnings("unused")
  private void example() {
    B.staticTwo();
    B.staticTwo();
    org.alfasoftware.astra.exampleTypes.B.staticTwo();
    org.alfasoftware.astra.exampleTypes.B.staticTwo();
    staticTwo();
    staticTwo();
  }
}
