package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

public class SwitchCaseSemicolonAfter {

  void switchStatement(int x) {
    switch (x) {
      case 1:
        System.out.println("one");
        break;
      case 2:
        break;
      default:
        break;
    }
  }
}
