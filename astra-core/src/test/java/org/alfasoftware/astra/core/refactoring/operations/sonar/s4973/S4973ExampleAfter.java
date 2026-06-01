package org.alfasoftware.astra.core.refactoring.operations.sonar.s4973;

public class S4973ExampleAfter {

  boolean stringEquals(String s1, String s2) {
    return s1.equals(s2);
  }

  boolean stringNotEquals(String s1, String s2) {
    return ! s1.equals(s2);
  }

  boolean integerEquals(Integer i1, Integer i2) {
    return i1.equals(i2);
  }

  boolean longNotEquals(Long l1, Long l2) {
    return ! l1.equals(l2);
  }

  boolean booleanEquals(Boolean b1, Boolean b2) {
    return b1.equals(b2);
  }

  boolean literalOnRight(String s) {
    return "hello".equals(s);
  }

  boolean literalOnLeft(String s) {
    return "hello".equals(s);
  }

  boolean nullCheck(String s) {
    return s == null;
  }

  boolean primitiveCheck(int i1, int i2) {
    return i1 == i2;
  }
}
