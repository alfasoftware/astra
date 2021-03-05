package org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype;

import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.bar.Bar;
import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.bar.CallerBar;
import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar.CallerFooBar;
import org.alfasoftware.astra.core.refactoring.methods.methoddeclaringtype.foobar.FooBar;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodDeclaringTypeRefactor;
import org.junit.Test;

public class TestMethodDeclaringTypeRefactor extends AbstractRefactorTest {

  /**
   * This tests refactoring the instance type on which a method is invoked.
   */
  @Test
  public void testMethodDeclaringTypeRefactor() {
    assertRefactor(CallerBar.class,
      new HashSet<>(Arrays.asList(
        MethodDeclaringTypeRefactor
          .forMethod(
            MethodMatcher.builder()
            .withFullyQualifiedDeclaringType(FooBar.class.getName())
            .withMethodName("doBar")
            .build())
        .toType(Bar.class.getName()))));
  }

  /**
   * This tests refactoring the instance type on which a method is invoked,
   * and that an import for the new type is added to the file.
   */
  @Test
  public void testMethodDeclaringTypeRefactorWithImport() {
    assertRefactor(CallerFooBar.class,
      new HashSet<>(Arrays.asList(
        MethodDeclaringTypeRefactor
          .forMethod(
            MethodMatcher.builder()
            .withFullyQualifiedDeclaringType(FooBar.class.getName())
            .withMethodName("doBar")
            .build())
        .toType(Bar.class.getName()))));
  }
}
