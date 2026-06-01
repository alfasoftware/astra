package org.alfasoftware.astra.core.refactoring.operations.sonar.s4201;

public class NullCheckInstanceofNoopExampleAfter {

  // Different variables — must not be rewritten
  void differentVariables(Object obj, Object other) {
    if (obj != null && other instanceof String) {
      System.out.println("different vars");
    }
  }

  // x == null || x instanceof Foo — semantics differ (true when null, but instanceof is false for null)
  void nullOrInstanceof(Object obj) {
    if (obj == null || obj instanceof String) {
      System.out.println("null or string");
    }
  }

  // x != null && !(x instanceof Foo) — semantics differ
  void notNullAndNotInstanceof(Object obj) {
    if (obj != null && !(obj instanceof String)) {
      System.out.println("not null and not string");
    }
  }

  // Standalone null check with no instanceof — nothing to do
  void justNullCheck(Object obj) {
    if (obj != null) {
      System.out.println("not null");
    }
  }

}
