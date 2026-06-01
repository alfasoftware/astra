package org.alfasoftware.astra.core.refactoring.operations.sonar.s4201;

import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestNullCheckInstanceofOperation extends AbstractRefactorTest {

  private static final Set<NullCheckInstanceofOperation> OPERATION =
      Set.of(new NullCheckInstanceofOperation());

  /**
   * All rewritable forms:
   * - {@code x != null && x instanceof Foo} (null check first or second, literal null first or second)
   * - {@code x == null || !(x instanceof Foo)} (null check first or second, literal null first or second)
   */
  @Test
  public void testRedundantNullChecks() {
    assertRefactor(NullCheckInstanceofExample.class, OPERATION);
  }

  /**
   * Cases that must NOT be rewritten:
   * - different variables in null check and instanceof
   * - semantically non-equivalent combinations ({@code == null || instanceof}, {@code != null && !instanceof})
   * - standalone null check with no instanceof
   */
  @Test
  public void testNoopCases() {
    assertRefactor(NullCheckInstanceofNoopExample.class, OPERATION);
  }
}
