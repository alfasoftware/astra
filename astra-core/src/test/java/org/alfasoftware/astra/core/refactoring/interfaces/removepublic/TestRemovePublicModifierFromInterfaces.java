package org.alfasoftware.astra.core.refactoring.interfaces.removepublic;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.interfaces.RemovePublicModifierFromInterfaces;
import org.junit.Test;

public class TestRemovePublicModifierFromInterfaces extends AbstractRefactorTest {
  @Test
  public void test() {
    assertRefactor(PublicInterface.class, new HashSet<>(Arrays.asList(new RemovePublicModifierFromInterfaces())));
  }
}
