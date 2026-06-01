package org.alfasoftware.astra.core.refactoring.operations.sonar.s4973;

public class S4973Example {

  boolean stringEquals(String s1, String s2) {
    return s1 == s2;
  }

  boolean stringNotEquals(String s1, String s2) {
    return s1 != s2;
  }

  boolean integerEquals(Integer i1, Integer i2) {
    return i1 == i2;
  }

  boolean longNotEquals(Long l1, Long l2) {
    return l1 != l2;
  }

  boolean booleanEquals(Boolean b1, Boolean b2) {
    return b1 == b2;
  }

  boolean literalOnRight(String s) {
    return s == "hello";
  }

  boolean literalOnLeft(String s) {
    return "hello" == s;
  }

  boolean nullCheck(String s) {
    return s == null;
  }

  boolean primitiveCheck(int i1, int i2) {
    return i1 == i2;
  }
}
