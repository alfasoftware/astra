package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import org.alfasoftware.astra.exampleTypes.A;

public class PatternMatchingInstanceofWhileExampleAfter {

  public void whileLoop(Object obj) {
    while (obj instanceof A a) {
      a.one();
    }
  }
}
