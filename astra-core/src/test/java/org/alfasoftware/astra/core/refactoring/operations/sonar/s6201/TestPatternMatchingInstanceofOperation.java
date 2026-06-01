package org.alfasoftware.astra.core.refactoring.operations.sonar.s6201;

import java.util.Set;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestPatternMatchingInstanceofOperation extends AbstractRefactorTest {

  private static final Set<PatternMatchingInstanceofOperation> OPERATION =
      Set.of(new PatternMatchingInstanceofOperation());

  /**
   * Basic if-statement cases:
   * - single use of cast variable
   * - multiple uses of cast variable
   * - cast declaration not the first statement in the block
   */
  @Test
  public void testIfStatement() {
    assertRefactor(PatternMatchingInstanceofIfExample.class, OPERATION);
  }

  /**
   * while-loop body containing a cast declaration of the instanceof subject.
   */
  @Test
  public void testWhileLoop() {
    assertRefactor(PatternMatchingInstanceofWhileExample.class, OPERATION);
  }

  /**
   * Ternary (conditional expression) where the then-expression contains a cast
   * of the instanceof subject.  A case with no matching cast is left unchanged.
   */
  @Test
  public void testTernary() {
    assertRefactor(PatternMatchingInstanceofTernaryExample.class, OPERATION);
  }

  /**
   * Cases that must NOT be rewritten:
   * - already uses pattern-matching instanceof
   * - negated instanceof with cast in the else branch
   * - cast variable is re-assigned after declaration
   * - instanceof subject is re-assigned before the cast declaration
   * - no cast expression in the block at all
   */
  @Test
  public void testNoopCases() {
    assertRefactor(PatternMatchingInstanceofNoopExample.class, OPERATION);
  }
}
