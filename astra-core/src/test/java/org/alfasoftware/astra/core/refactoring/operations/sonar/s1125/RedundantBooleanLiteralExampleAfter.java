package org.alfasoftware.astra.core.refactoring.operations.sonar.s1125;

public class RedundantBooleanLiteralExampleAfter {

  void equalsTrue(boolean bool) {
    if (bool) {
      System.out.println("bool is true");
    }
  }

  void equalsFalse(boolean bool) {
    if (! bool) {
      System.out.println("bool is false");
    }
  }

  void notEqualsTrue(boolean bool) {
    if (! bool) {
      System.out.println("bool is false");
    }
  }

  void notEqualsFalse(boolean bool) {
    if (bool) {
      System.out.println("bool is true");
    }
  }

  void trueEquals(boolean bool) {
    if (bool) {
      System.out.println("bool is true");
    }
  }

  void falseEquals(boolean bool) {
    if (! bool) {
      System.out.println("bool is false");
    }
  }

  void trueNotEquals(boolean bool) {
    if (! bool) {
      System.out.println("bool is false");
    }
  }

  void falseNotEquals(boolean bool) {
    if (bool) {
      System.out.println("bool is true");
    }
  }

  void equalsTrueBoxed(Boolean bool) {
    if (bool) {
      System.out.println("bool is true");
    }
  }

  void equalsFalseBoxed(Boolean bool) {
    if (! bool) {
      System.out.println("bool is false");
    }
  }
}
