package org.alfasoftware.astra.core.refactoring.imports;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.imports.UnusedImportRefactor;
import org.junit.Test;

public class TestImportsRefactor extends AbstractRefactorTest {

  @Test
  public void testUnusedImportRefactor() {
    assertRefactor(UnusedImportExample.class,
      new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }

  @Test
  public void testImportReordering() {
    assertRefactor(ImportsReorderingExample.class,
      new HashSet<>(Arrays.asList(new UnusedImportRefactor())));
  }
}
