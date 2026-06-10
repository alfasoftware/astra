package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

public class BlockLevelEmptyStatement {

  static {
    ;
    System.out.println("static init");
  }

  {
    ;
    System.out.println("instance init");
  }

  void singleSemicolon() {
    ;
    doSomething();
  }

  void multipleSemicolons() {
    ;
    ;
    doSomething();
    ;
  }

  void semicolonInIfBlock() {
    if (true) {
      ;
      doSomething();
    }
  }

  void semicolonInForBlock() {
    for (int i = 0; i < 10; i++) {
      ;
    }
  }

  void doSomething() {}
}
