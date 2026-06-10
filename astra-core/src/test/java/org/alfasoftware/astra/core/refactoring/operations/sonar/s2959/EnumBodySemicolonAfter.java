package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

public class EnumBodySemicolonAfter {

  enum WithMethodsAndSemicolon {
    RED, GREEN, BLUE;

    public String label() {
      return name().toLowerCase();
    }
  }

  enum WithNoBodyDeclarations {
    ONE, TWO, THREE
  }
}
