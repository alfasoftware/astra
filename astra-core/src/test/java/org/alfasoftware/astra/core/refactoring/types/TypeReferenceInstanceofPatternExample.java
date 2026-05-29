package org.alfasoftware.astra.core.refactoring.types;

import org.alfasoftware.astra.exampleTypes.A;

public class TypeReferenceInstanceofPatternExample {

  @SuppressWarnings("unused")
  public void doFoo(Object o) {
    if (o instanceof A a) {
      a.one();
    }
  }
}
