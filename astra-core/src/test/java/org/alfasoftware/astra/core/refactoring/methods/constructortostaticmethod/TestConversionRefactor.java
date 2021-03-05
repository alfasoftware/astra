package org.alfasoftware.astra.core.refactoring.methods.constructortostaticmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.operations.methods.ConstructorToStaticMethodRefactor;
import org.junit.Test;

public class TestConversionRefactor extends AbstractRefactorTest {

  @Test
  public void testConversionFromConstructorToStaticMethod() {
    assertRefactor(ConstructorToStaticMethodExample.class,
      new HashSet<>(Arrays.asList(
        new ConstructorToStaticMethodRefactor(
          MethodMatcher.builder()
            .withMethodName("Date")
            .withFullyQualifiedDeclaringType("java.util.Date")
            .withFullyQualifiedParameters(new ArrayList<>()) // empty constructor
            .build(),
          "java.time.LocalDate", "now"))));
  }

  @Test
  public void testConversionFromConstructorToStaticMethodWithArgs() {
    assertRefactor(ConstructorToStaticMethodWithArgsExample.class,
      new HashSet<>(Arrays.asList(
        new ConstructorToStaticMethodRefactor(
          MethodMatcher.builder()
            .withMethodName("Date")
            .withFullyQualifiedDeclaringType("java.util.Date")
            .withFullyQualifiedParameters(Arrays.asList("int", "int", "int")) // year month day
            .build(),
          "java.time.LocalDate", "of"))));
  }

}
