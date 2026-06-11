package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import static org.junit.Assert.*;

public class AssertTrueStaticImportExample {

  // Wildcard static import — no new import needed; just rename and rewrite args
  void trueNullCheck(Object a) {
    assertTrue(a == null);
  }

  void falseNullCheck(Object a) {
    assertFalse(a == null);
  }

  void trueEqualsMethod(String a, String b) {
    assertTrue(a.equals(b));
  }

  void truePrimitiveEquals(int a, int b) {
    assertTrue(a == b);
  }
}
