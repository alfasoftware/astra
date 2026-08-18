package org.alfasoftware.astra.core.refactoring.operations.sonar.s1125;

import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestRedundantBooleanLiteralRefactor extends AbstractRefactorTest {

  private static final Set<RedundantBooleanLiteralRefactor> OPERATION =
      Set.of(new RedundantBooleanLiteralRefactor());

  /**
   * Test rewriting all redundant boolean literal combinations.
   */
  @Test
  public void testRedundantBooleanLiterals() {
    assertRefactor(RedundantBooleanLiteralExample.class, OPERATION);
  }

  /**
   * Test cases that should not be refactored.
   */
  @Test
  public void testNoopCases() {
    assertRefactor(RedundantBooleanLiteralNoopExample.class, OPERATION);
  }
}
