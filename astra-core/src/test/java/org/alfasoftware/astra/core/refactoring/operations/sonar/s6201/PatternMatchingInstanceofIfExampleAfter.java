package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import org.alfasoftware.astra.exampleTypes.A;

public class PatternMatchingInstanceofIfExampleAfter {

  public void basicIf(Object obj) {
    if (obj instanceof A a) {
      a.one();
    }
  }

  public void multipleUsesOfCastVar(Object obj) {
    if (obj instanceof A a) {
      a.one();
      a.one();
    }
  }

  public void castNotFirstStatement(Object obj) {
    if (obj instanceof A a) {
      System.out.println("checking");
      a.one();
    }
  }

  public void parenthesizedCondition(Object obj) {
    if ((obj instanceof A a)) {
      a.one();
    }
  }
}
