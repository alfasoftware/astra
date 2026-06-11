package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

enum TopLevelEnumAnnotatedConstantsAfter {

  @Deprecated
  ALPHA,

  @Deprecated
  BETA;

  public String label() {
    return name().toLowerCase();
  }
}
