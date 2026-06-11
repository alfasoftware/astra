package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import static org.junit.Assert.*;

public class AssertTrueStaticImportExampleAfter {

  // Wildcard static import — no new import needed; just rename and rewrite args
  void trueNullCheck(Object a) {
    assertNull(a);
  }

  void falseNullCheck(Object a) {
    assertNotNull(a);
  }

  void trueEqualsMethod(String a, String b) {
    assertEquals(a, b);
  }

  void truePrimitiveEquals(int a, int b) {
    assertEquals(a, b);
  }
}
