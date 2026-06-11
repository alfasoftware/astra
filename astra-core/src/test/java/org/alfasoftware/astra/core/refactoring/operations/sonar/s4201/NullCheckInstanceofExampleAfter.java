package org.alfasoftware.astra.core.refactoring.operations.sonar.s4201;

public class NullCheckInstanceofExampleAfter {

  // x != null && x instanceof Foo
  void notNullAndInstanceof(Object obj) {
    if (obj instanceof String) {
      System.out.println("is string");
    }
  }

  // instanceof first, then != null
  void instanceofAndNotNull(Object obj) {
    if (obj instanceof String) {
      System.out.println("is string");
    }
  }

  // null literal on the left: null != x && x instanceof Foo
  void nullNotEqualsAndInstanceof(Object obj) {
    if (obj instanceof String) {
      System.out.println("is string");
    }
  }

  // instanceof first, null literal check on right: x instanceof Foo && null != x
  void instanceofAndNullNotEquals(Object obj) {
    if (obj instanceof String) {
      System.out.println("is string");
    }
  }

  // x == null || !(x instanceof Foo)
  void nullOrNotInstanceof(Object obj) {
    if (!(obj instanceof String)) {
      System.out.println("not a string");
    }
  }

  // !(instanceof) first, then == null
  void notInstanceofOrNull(Object obj) {
    if (!(obj instanceof String)) {
      System.out.println("not a string");
    }
  }

  // null literal on the left: null == x || !(x instanceof Foo)
  void nullEqualsOrNotInstanceof(Object obj) {
    if (!(obj instanceof String)) {
      System.out.println("not a string");
    }
  }

  // !(instanceof) first, null literal check: !(x instanceof Foo) || null == x
  void notInstanceofOrNullEquals(Object obj) {
    if (!(obj instanceof String)) {
      System.out.println("not a string");
    }
  }

  // null check inside a larger && chain: null check still removed from the inner sub-expression
  void nullCheckInLargerChain(Object obj, boolean extra) {
    if (obj instanceof String && extra) {
      System.out.println("extended chain");
    }
  }
}
