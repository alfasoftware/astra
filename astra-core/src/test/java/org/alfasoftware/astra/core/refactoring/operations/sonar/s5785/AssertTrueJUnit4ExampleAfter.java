package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import org.junit.Assert;

public class AssertTrueJUnit4ExampleAfter {

  // assertTrue(a == null) → assertNull(a)
  void trueNullCheckRight(Object a) {
    Assert.assertNull(a);
  }

  // assertTrue(null == a) → assertNull(a)  [null on the left]
  void trueNullCheckLeft(Object a) {
    Assert.assertNull(a);
  }

  // assertTrue(a != null) → assertNotNull(a)
  void trueNotNullCheck(Object a) {
    Assert.assertNotNull(a);
  }

  // assertFalse(a == null) → assertNotNull(a)
  void falseNullCheck(Object a) {
    Assert.assertNotNull(a);
  }

  // assertFalse(a != null) → assertNull(a)
  void falseNotNullCheck(Object a) {
    Assert.assertNull(a);
  }

  // assertTrue(a.equals(b)) → assertEquals(a, b)
  void trueEqualsMethod(String a, String b) {
    Assert.assertEquals(a, b);
  }

  // assertFalse(a.equals(b)) → assertNotEquals(a, b)
  void falseEqualsMethod(String a, String b) {
    Assert.assertNotEquals(a, b);
  }

  // assertTrue(Objects.equals(a, b)) → assertEquals(a, b)
  void trueObjectsEquals(Object a, Object b) {
    Assert.assertEquals(a, b);
  }

  // assertTrue(a == b) with primitives → assertEquals(a, b)
  void truePrimitiveEquals(int a, int b) {
    Assert.assertEquals(a, b);
  }

  // assertTrue(a != b) with primitives → assertNotEquals(a, b)
  void truePrimitiveNotEquals(int a, int b) {
    Assert.assertNotEquals(a, b);
  }

  // assertTrue(a == b) with objects (reference equality) → assertSame(a, b)
  void trueObjectSame(Object a, Object b) {
    Assert.assertSame(a, b);
  }

  // assertTrue(a != b) with objects (reference inequality) → assertNotSame(a, b)
  void trueObjectNotSame(Object a, Object b) {
    Assert.assertNotSame(a, b);
  }

  // assertTrue(!a.equals(b)) → assertNotEquals(a, b)  [NOT unwrapping]
  void trueNegatedEquals(String a, String b) {
    Assert.assertNotEquals(a, b);
  }

  // assertFalse(!a.equals(b)) → assertEquals(a, b)  [double inversion]
  void falseNegatedEquals(String a, String b) {
    Assert.assertEquals(a, b);
  }

  // assertTrue with message (JUnit 4: message is first arg) → assertNull with message first
  void trueWithMessage(Object a) {
    Assert.assertNull("should be null", a);
  }

  // assertTrue equals with message → assertEquals with message first
  void trueEqualsWithMessage(String a, String b) {
    Assert.assertEquals("should be equal", a, b);
  }

  // assertFalse equals with message → assertNotEquals with message first
  void falseEqualsWithMessage(String a, String b) {
    Assert.assertNotEquals("should not be equal", a, b);
  }
}
