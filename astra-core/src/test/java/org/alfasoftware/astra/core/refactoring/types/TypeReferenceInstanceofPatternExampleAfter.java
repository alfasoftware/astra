package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.B;

public class TypeReferenceInstanceofPatternExampleAfter {

  @SuppressWarnings("unused")
  public void doFoo(Object o) {
    if (o instanceof B a) {
      a.one();
    }
  }
}
