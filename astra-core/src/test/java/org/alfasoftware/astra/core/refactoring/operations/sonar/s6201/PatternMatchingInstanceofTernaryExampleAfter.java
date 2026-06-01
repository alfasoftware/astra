package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

public class PatternMatchingInstanceofTernaryExampleAfter {

  public String ternary(Object obj) {
    return obj instanceof String string ? string.toUpperCase() : "";
  }

  public int ternaryNoCast(Object obj) {
    return obj instanceof String ? obj.hashCode() : 0;
  }
}
