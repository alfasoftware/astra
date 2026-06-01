package org.alfasoftware.astra.core.refactoring.operations.sonar.s4973;

import java.util.Collections;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.junit.Test;

public class TestStringAndBoxedTypeComparisonRefactor extends AbstractRefactorTest {

  @Test
  public void testS4973AllCases() {
    assertRefactor(
        S4973Example.class,
        Collections.singleton(new StringAndBoxedTypeComparisonRefactor()));
  }
}
