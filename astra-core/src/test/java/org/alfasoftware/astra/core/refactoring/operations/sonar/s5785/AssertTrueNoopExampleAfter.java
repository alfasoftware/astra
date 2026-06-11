package org.alfasoftware.astra.core.refactoring.operations.sonar.s5785;

import org.junit.Assert;

public class AssertTrueNoopExampleAfter {

  // Plain boolean variable — no dedicated assertion can be determined
  void plainBoolean(boolean flag) {
    Assert.assertTrue(flag);
  }

  // Non-equals method call — not covered by the rule
  void nonEqualsMethod(String s) {
    Assert.assertTrue(s.isEmpty());
  }

  // Compound boolean expression (&&) — no dedicated assertion
  void compoundAnd(boolean a, boolean b) {
    Assert.assertTrue(a && b);
  }

  // Compound boolean expression (||) — no dedicated assertion
  void compoundOr(boolean a, boolean b) {
    Assert.assertTrue(a || b);
  }

  // assertFalse with plain boolean — no simplification
  void plainBooleanFalse(boolean flag) {
    Assert.assertFalse(flag);
  }

  // assertFalse with non-equals method — no simplification
  void nonEqualsMethodFalse(String s) {
    Assert.assertFalse(s.isEmpty());
  }
}
