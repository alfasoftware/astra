package org.alfasoftware.astra.core.refactoring.operations.sonar.s1125;

public class RedundantBooleanLiteralExample {

  void equalsTrue(boolean bool) {
    if (bool == true) {
      System.out.println("bool is true");
    }
  }

  void equalsFalse(boolean bool) {
    if (bool == false) {
      System.out.println("bool is false");
    }
  }

  void notEqualsTrue(boolean bool) {
    if (bool != true) {
      System.out.println("bool is false");
    }
  }

  void notEqualsFalse(boolean bool) {
    if (bool != false) {
      System.out.println("bool is true");
    }
  }

  void trueEquals(boolean bool) {
    if (true == bool) {
      System.out.println("bool is true");
    }
  }

  void falseEquals(boolean bool) {
    if (false == bool) {
      System.out.println("bool is false");
    }
  }

  void trueNotEquals(boolean bool) {
    if (true != bool) {
      System.out.println("bool is false");
    }
  }

  void falseNotEquals(boolean bool) {
    if (false != bool) {
      System.out.println("bool is true");
    }
  }

  void equalsTrueBoxed(Boolean bool) {
    if (bool == true) {
      System.out.println("bool is true");
    }
  }

  void equalsFalseBoxed(Boolean bool) {
    if (bool == false) {
      System.out.println("bool is false");
    }
  }
}
