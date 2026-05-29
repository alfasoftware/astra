package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.DescribedPredicate.describedPredicate;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

/**
 * Tests that the {@link MethodMatcher} can match by return type for method invocations
 * (and constructors), not just method declarations.
 *
 * <p>Return-type matching on invocations and constructors requires the method binding to be
 * resolvable, so these tests supply the test sources on the classpath in the same way as the
 * existing binding-dependent {@link TestMethodMatcher} tests.</p>
 */
public class TestMethodMatcherReturnTypeForInvocations {

  private static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
  private static final String TEST_EXAMPLES = "./src/test/java";


  /**
   * A method invocation should match when its resolved return type is the configured one.
   */
  @Test
  public void testInvocationMatchesWhenReturnTypeMatches() {
    MethodMatcher returnsStringMatcher = MethodMatcher.builder()
        .withMethodName("baseMethod")
        .withReturnType(describedPredicate("Return type should be String", s -> s.equals(String.class.getName())))
        .build();

    checkMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingInvocationWithReturnTypeString.class);
  }


  /**
   * The exact-string return type form should work for invocations too.
   */
  @Test
  public void testInvocationMatchesWhenReturnTypeMatchesExactString() {
    MethodMatcher returnsStringMatcher = MethodMatcher.builder()
        .withMethodName("baseMethod")
        .withfullyQualifiedReturnType(String.class.getName())
        .build();

    checkMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingInvocationWithReturnTypeString.class);
  }


  /**
   * A method invocation should not match when its resolved return type differs from the configured one.
   */
  @Test
  public void testInvocationDoesNotMatchWhenReturnTypeDiffers() {
    MethodMatcher returnsStringMatcher = MethodMatcher.builder()
        .withMethodName("length")
        .withReturnType(describedPredicate("Return type should be String", s -> s.equals(String.class.getName())))
        .build();

    // length() returns int, so the String return-type matcher must not match it.
    checkNoMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingInvocationWithReturnTypeInt.class);
  }


  /**
   * A void-returning invocation should match a matcher configured with a void return type.
   */
  @Test
  public void testVoidInvocationMatchesVoidReturnType() {
    MethodMatcher returnsVoidMatcher = MethodMatcher.builder()
        .withMethodName("trimToSize")
        .withReturnType(describedPredicate("Return type should be void", s -> s.equals("void")))
        .build();

    checkMethodMatchFoundInClass(returnsVoidMatcher, ExampleClassUsingInvocationWithReturnTypeVoid.class);
  }


  /**
   * A void-returning invocation should not match a non-void return type matcher.
   */
  @Test
  public void testVoidInvocationDoesNotMatchStringReturnType() {
    MethodMatcher returnsStringMatcher = MethodMatcher.builder()
        .withMethodName("trimToSize")
        .withReturnType(describedPredicate("Return type should be String", s -> s.equals(String.class.getName())))
        .build();

    checkNoMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingInvocationWithReturnTypeVoid.class);
  }


  /**
   * A constructor (class instance creation) reports a void return type in JDT,
   * so it should match a void return-type matcher.
   */
  @Test
  public void testConstructorMatchesVoidReturnType() {
    MethodMatcher constructorMatcher = MethodMatcher.builder()
        .withMethodName("ExampleUsedClass")
        .withReturnType(describedPredicate("Return type should be void", s -> s.equals("void")))
        .build();

    checkMethodMatchFoundInClass(constructorMatcher, ExampleClassUsingConstructorWithVoidReturnType.class);
  }


  /**
   * Regression test: return-type matching on method declarations must still work.
   */
  @Test
  public void testMethodDeclarationReturnTypeStillMatches() {
    MethodMatcher returnsStringMatcher = MethodMatcher.builder()
        .withReturnType(describedPredicate("Return type should be String", s -> s.equals(String.class.getName())))
        .build();

    checkMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingMethodWithReturnTypeString.class);
    checkNoMethodMatchFoundInClass(returnsStringMatcher, ExampleClassUsingMethodWithReturnTypeInt.class);
  }


  private void checkMethodMatchFoundInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest) {
    checkMethodMatchInClass(methodMatcherToTest, clazzToTest, true);
  }

  private void checkNoMethodMatchFoundInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest) {
    checkMethodMatchInClass(methodMatcherToTest, clazzToTest, false);
  }


  private void checkMethodMatchInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest, boolean shouldFindMatch) {
    File fileToTest = new File(TEST_EXAMPLES + "/" + clazzToTest.getName().replaceAll("\\.", "/") + ".java");
    String fileContentBefore = null;
    try {
      fileContentBefore = new String(Files.readAllBytes(fileToTest.toPath()));
    } catch (IOException e) {
      fail("IOException when reading file : " + e);
    }

    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(Path.of(""), fileContentBefore, new String[]{TEST_SOURCE}, new String[0]);

    final ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);

    for (MethodInvocation methodInvocation : visitor.getMethodInvocations()) {
      if (methodMatcherToTest.matches(methodInvocation, compilationUnit)) {
        if (shouldFindMatch) {
          return;
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + methodInvocation.toString() + "]");
        }
      }
    }
    for (MethodDeclaration methodDeclaration : visitor.getMethodDeclarations()) {
      if (methodMatcherToTest.matches(methodDeclaration)) {
        if (shouldFindMatch) {
          return;
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + methodDeclaration.toString() + "]");
        }
      }
    }
    for (ClassInstanceCreation classInstanceCreation : visitor.getClassInstanceCreations()) {
      if (methodMatcherToTest.matches(classInstanceCreation)) {
        if (shouldFindMatch) {
          return;
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + classInstanceCreation.toString() + "]");
        }
      }
    }
    if (shouldFindMatch) {
      fail("Should have found a match for [" + methodMatcherToTest + "] within [" + fileContentBefore + "]");
    }
  }
}
