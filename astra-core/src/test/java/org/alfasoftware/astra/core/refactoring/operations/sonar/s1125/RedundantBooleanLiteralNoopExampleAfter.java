package org.alfasoftware.astra.core.refactoring.operations.sonar.s1125;

public class RedundantBooleanLiteralNoopExampleAfter {

  void integerComparison(int x) {
    if (x == 1) {
      System.out.println("x is 1");
    }
  }

  void objectComparison(Object obj) {
    if (obj == null) {
      System.out.println("obj is null");
    }
  }

  void twoBooleans(boolean a, boolean b) {
    if (a == b) {
      System.out.println("a equals b");
    }
  }
}
