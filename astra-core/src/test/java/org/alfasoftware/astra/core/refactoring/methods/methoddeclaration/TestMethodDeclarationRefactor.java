package org.alfasoftware.astra.core.refactoring.methods.methoddeclaration;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.methods.RemoveMethodDeclarationRefactor;
import org.junit.Test;

public class TestMethodDeclarationRefactor extends AbstractRefactorTest {

  @Test
  public void testRemoveMethodDeclaration() {
    assertRefactor(RemoveMethodDeclarationExample.class,
      new HashSet<>(Arrays.asList(new RemoveMethodDeclarationRefactor(
        MethodMatcher.builder()
        .withMethodName("doThing")
        .withFullyQualifiedDeclaringType(RemoveMethodDeclarationExample.class.getName())
        .withFullyQualifiedParameters(Arrays.asList("java.lang.String"))
        .build()))));
  }

}
