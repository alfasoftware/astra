package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

public class NestedTypeSemicolonAfter {

  static class StaticNested {
    void foo() {
    }
  }

  class Inner {
    int x = 1;
  }

  interface InnerInterface {
    void method();
  }
}
