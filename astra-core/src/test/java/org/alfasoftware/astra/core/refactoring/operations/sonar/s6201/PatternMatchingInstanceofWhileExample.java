package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import org.alfasoftware.astra.exampleTypes.A;

public class PatternMatchingInstanceofWhileExample {

  public void whileLoop(Object obj) {
    while (obj instanceof A) {
      A a = (A) obj;
      a.one();
    }
  }
}
