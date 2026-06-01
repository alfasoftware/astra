package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import org.alfasoftware.astra.exampleTypes.A;

public class PatternMatchingInstanceofIfExample {

  public void basicIf(Object obj) {
    if (obj instanceof A) {
      A a = (A) obj;
      a.one();
    }
  }

  public void multipleUsesOfCastVar(Object obj) {
    if (obj instanceof A) {
      A a = (A) obj;
      a.one();
      a.one();
    }
  }

  public void castNotFirstStatement(Object obj) {
    if (obj instanceof A) {
      System.out.println("checking");
      A a = (A) obj;
      a.one();
    }
  }

  public void parenthesizedCondition(Object obj) {
    if ((obj instanceof A)) {
      A a = (A) obj;
      a.one();
    }
  }
}
