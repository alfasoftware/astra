package org.alfasoftware.astra.core.refactoring.operations.sonar.s1155;

import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.sonar.s1155.CollectionIsEmptyRefactor;
import org.junit.Test;

public class TestCollectionIsEmptyRefactor extends AbstractRefactorTest {

  /**
   * All six size()-comparison forms in both normal and reversed operand order
   * are replaced with isEmpty() or !isEmpty() as appropriate.
   */
  @Test
  public void testAllForms() {
    assertRefactor(CollectionIsEmptyExample.class,
        Collections.singleton(new CollectionIsEmptyRefactor()));
  }

  /**
   * Non-Collection.size() calls, already-correct isEmpty() calls, and
   * size() comparisons against literals other than 0/1 are left unchanged.
   */
  @Test
  public void testNoOpCases() {
    assertRefactor(CollectionIsEmptyNonCollectionExample.class,
        Collections.singleton(new CollectionIsEmptyRefactor()));
  }
}
