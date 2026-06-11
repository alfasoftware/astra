package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import java.util.Objects;
import org.junit.Assert;

public class AssertTrueJUnit4Example {

  // assertTrue(a == null) → assertNull(a)
  void trueNullCheckRight(Object a) {
    Assert.assertTrue(a == null);
  }

  // assertTrue(null == a) → assertNull(a)  [null on the left]
  void trueNullCheckLeft(Object a) {
    Assert.assertTrue(null == a);
  }

  // assertTrue(a != null) → assertNotNull(a)
  void trueNotNullCheck(Object a) {
    Assert.assertTrue(a != null);
  }

  // assertFalse(a == null) → assertNotNull(a)
  void falseNullCheck(Object a) {
    Assert.assertFalse(a == null);
  }

  // assertFalse(a != null) → assertNull(a)
  void falseNotNullCheck(Object a) {
    Assert.assertFalse(a != null);
  }

  // assertTrue(a.equals(b)) → assertEquals(a, b)
  void trueEqualsMethod(String a, String b) {
    Assert.assertTrue(a.equals(b));
  }

  // assertFalse(a.equals(b)) → assertNotEquals(a, b)
  void falseEqualsMethod(String a, String b) {
    Assert.assertFalse(a.equals(b));
  }

  // assertTrue(Objects.equals(a, b)) → assertEquals(a, b)
  void trueObjectsEquals(Object a, Object b) {
    Assert.assertTrue(Objects.equals(a, b));
  }

  // assertTrue(a == b) with primitives → assertEquals(a, b)
  void truePrimitiveEquals(int a, int b) {
    Assert.assertTrue(a == b);
  }

  // assertTrue(a != b) with primitives → assertNotEquals(a, b)
  void truePrimitiveNotEquals(int a, int b) {
    Assert.assertTrue(a != b);
  }

  // assertTrue(a == b) with objects (reference equality) → assertSame(a, b)
  void trueObjectSame(Object a, Object b) {
    Assert.assertTrue(a == b);
  }

  // assertTrue(a != b) with objects (reference inequality) → assertNotSame(a, b)
  void trueObjectNotSame(Object a, Object b) {
    Assert.assertTrue(a != b);
  }

  // assertTrue(!a.equals(b)) → assertNotEquals(a, b)  [NOT unwrapping]
  void trueNegatedEquals(String a, String b) {
    Assert.assertTrue(!a.equals(b));
  }

  // assertFalse(!a.equals(b)) → assertEquals(a, b)  [double inversion]
  void falseNegatedEquals(String a, String b) {
    Assert.assertFalse(!a.equals(b));
  }

  // assertTrue with message (JUnit 4: message is first arg) → assertNull with message first
  void trueWithMessage(Object a) {
    Assert.assertTrue("should be null", a == null);
  }

  // assertTrue equals with message → assertEquals with message first
  void trueEqualsWithMessage(String a, String b) {
    Assert.assertTrue("should be equal", a.equals(b));
  }

  // assertFalse equals with message → assertNotEquals with message first
  void falseEqualsWithMessage(String a, String b) {
    Assert.assertFalse("should not be equal", a.equals(b));
  }
}
