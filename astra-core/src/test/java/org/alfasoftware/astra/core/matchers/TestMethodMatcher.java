package org.alfasoftware.astra.core.matchers;

import static org.alfasoftware.astra.core.matchers.DescribedPredicate.describedPredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.utils.AstraUtils;
import org.alfasoftware.astra.core.utils.ClassVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

/**
 * Tests that the MethodMatcher correctly finds methods of interest with no false positives
 */
public class TestMethodMatcher {

  protected static final String TEST_SOURCE = Paths.get(".").toAbsolutePath().normalize().toString().concat("/src/test/java");
  protected static final String TEST_EXAMPLES = "./src/test/java";

  /**
   * Checks that we can match a no-arg method whether or not we provide a parameter list
   */
  @Test
  public void testMatchNoArgMethod() {

    MethodMatcher noArgMethodMatcherWithoutParamsSpecified = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("baseMethod")
        .build();

    MethodMatcher noArgMethodMatcherWithParamsSpecified = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("baseMethod")
        .withFullyQualifiedParameters(new ArrayList<>())
        .build();

    checkMethodMatchFoundInClass(noArgMethodMatcherWithoutParamsSpecified, ExampleClassUsingNoArgBaseMethod.class);
    checkMethodMatchFoundInClass(noArgMethodMatcherWithoutParamsSpecified, ExampleClassUsingOneArgBaseMethod.class); // finds match because we don't care about number of params
    checkNoMethodMatchFoundInClass(noArgMethodMatcherWithoutParamsSpecified, ExampleClassUsingOneArgMethod.class);

    checkMethodMatchFoundInClass(noArgMethodMatcherWithParamsSpecified, ExampleClassUsingNoArgBaseMethod.class);
    checkNoMethodMatchFoundInClass(noArgMethodMatcherWithParamsSpecified, ExampleClassUsingOneArgBaseMethod.class); // finds no match because we care about number of params
    checkNoMethodMatchFoundInClass(noArgMethodMatcherWithParamsSpecified, ExampleClassUsingOneArgMethod.class);
  }


  /**
   * Tests that we only match to a one-arg method when we provide the correct parameter type
   */
  @Test
  public void testMatchOneArgMethodWithParameterList() {

    MethodMatcher correctOneArgMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .withFullyQualifiedParameters(Arrays.asList("java.lang.Long"))
        .build();

    MethodMatcher incorrectOneArgMethodMatcher1 = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .withFullyQualifiedParameters(Arrays.asList("long"))
        .build();

    MethodMatcher incorrectOneArgMethodMatcher2 = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .withFullyQualifiedParameters(Arrays.asList("java.lang.String"))
        .build();

    checkMethodMatchFoundInClass(correctOneArgMethodMatcher, ExampleClassUsingOneArgMethod.class);
    checkNoMethodMatchFoundInClass(correctOneArgMethodMatcher, ExampleClassUsingNoArgBaseMethod.class);
    checkNoMethodMatchFoundInClass(incorrectOneArgMethodMatcher1, ExampleClassUsingOneArgMethod.class);
    checkNoMethodMatchFoundInClass(incorrectOneArgMethodMatcher2, ExampleClassUsingOneArgMethod.class);
  }


  /**
   * Tests that we can match to a the methods that have arguments without providing parameter types
   */
  @Test
  public void testMatchWithArgMethodWithoutParameterList() {

    MethodMatcher correctOneArgMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .build();

    checkMethodMatchFoundInClass(correctOneArgMethodMatcher, ExampleClassUsingOneArgMethod.class);
    checkMethodMatchFoundInClass(correctOneArgMethodMatcher, ExampleClassUsingVarargMethod.class);
    checkNoMethodMatchFoundInClass(correctOneArgMethodMatcher, ExampleClassUsingNoArgBaseMethod.class);
  }


  /**
   * Tests that we only match to a var-arg method when we provide the correct parameter type
   */
  @Test
  public void testMatchVarArgMethodWithParameterList() {

    MethodMatcher correctVarargMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .isVarargs(true)
        .build();

    MethodMatcher incorrectvarargMethodMatcher1 = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .withFullyQualifiedParameters(Arrays.asList("java.lang.String"))
        .build();

    checkMethodMatchFoundInClass(correctVarargMethodMatcher, ExampleClassUsingVarargMethod.class);
    checkNoMethodMatchFoundInClass(correctVarargMethodMatcher, ExampleClassUsingNoArgBaseMethod.class);
    checkNoMethodMatchFoundInClass(correctVarargMethodMatcher, ExampleClassUsingOneArgMethod.class);
    checkNoMethodMatchFoundInClass(incorrectvarargMethodMatcher1, ExampleClassUsingVarargMethod.class);
  }


  /**
   * Tests that we can match method fields using predicates like String's startsWith, contains and equals.
   */
  @Test
  public void testMatchPredicateTypeAndMethodAndReturnTypePredicate() {
    MethodMatcher isWrappedBooleanMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(describedPredicate("Is in a matchers package", s -> s.contains("matchers")))
        .withMethodName(describedPredicate("Name starts with 'is'", s -> s.startsWith("is")))
        .withReturnType(describedPredicate("Return type is Boolean", s -> s.equals(Boolean.class.getName())))
        .build();
    checkMethodMatchFoundInClass(isWrappedBooleanMatcher, ExamplePredicateMatches.class);
    checkNoMethodMatchFoundInClass(isWrappedBooleanMatcher, ExamplePredicateMethodNameMismatch.class);
    checkNoMethodMatchFoundInClass(isWrappedBooleanMatcher, ExamplePredicateReturnTypeMismatch.class);
  }


  /**
   * Tests that we only match to a method for an imported type when present.
   */
  @Test
  public void testMatchImportedTypeMethod() {

    MethodMatcher correctDateMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType("java.util.Date")
        .withMethodName("setTime")
        .build();

    checkMethodMatchFoundInClass(correctDateMethodMatcher, ExampleClassUsingMethodsWithImport.class);
    checkMethodMatchFoundInClass(correctDateMethodMatcher, ExampleClassUsingMethodsWithFullyQualifiedName.class);
    checkNoMethodMatchFoundInClass(correctDateMethodMatcher, ExampleClassUsingOneArgMethod.class);
  }


  /**
   * Tests that we match to a method for an imported type when present even if the method is on a super-type
   */
  @Test
  public void testMatchImportedTypeMethodFromSuperClass() {

    MethodMatcher correctObjectMethodCalledOnDateMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType("java.util.Date")
        .withMethodName("notify")
        .build();

    checkMethodMatchFoundInClass(correctObjectMethodCalledOnDateMatcher, ExampleClassUsingMethodsWithImport.class);
    checkMethodMatchFoundInClass(correctObjectMethodCalledOnDateMatcher, ExampleClassUsingMethodsWithFullyQualifiedName.class);
    checkNoMethodMatchFoundInClass(correctObjectMethodCalledOnDateMatcher, ExampleClassUsingOneArgMethod.class);
  }


  /**
   * Tests that we only match to a method that is statically imported, so only the method name is present in the body of the code.
   */
  @Test
  public void testMatchStaticallyImportedMethod() {

    MethodMatcher correctCurrencyGetInstanceMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType("java.util.Currency")
        .withMethodName("getInstance")
        .build();

    MethodMatcher incorrectLocaleGetInstanceMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType("java.util.Locale")
        .withMethodName("getInstance")
        .build();

    checkMethodMatchFoundInClass(correctCurrencyGetInstanceMethodMatcher, ExampleClassUsingMethodsWithStaticImport.class);
    checkNoMethodMatchFoundInClass(correctCurrencyGetInstanceMethodMatcher, ExampleClassUsingOneArgMethod.class);
    checkNoMethodMatchFoundInClass(incorrectLocaleGetInstanceMethodMatcher, ExampleClassUsingMethodsWithStaticImport.class);
  }


  /**
   * Tests that we can match to a static method when we provide the correct parameter types
   */
  @Test
  public void testMatchStaticMethodWithParameterList() {

    MethodMatcher correctStaticMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("staticMethod")
        .withFullyQualifiedParameters(Arrays.asList("char", "java.lang.Integer"))
        .build();

    MethodMatcher incorrectStaticMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("staticMethod")
        .withFullyQualifiedParameters(Arrays.asList("char", "int"))
        .build();

    checkMethodMatchFoundInClass(correctStaticMethodMatcher, ExampleClassUsingStaticMethod.class);
    checkNoMethodMatchFoundInClass(correctStaticMethodMatcher, ExampleClassUsingNoArgBaseMethod.class);
    checkNoMethodMatchFoundInClass(correctStaticMethodMatcher, ExampleClassUsingOneArgMethod.class);
    checkNoMethodMatchFoundInClass(correctStaticMethodMatcher, ExampleClassUsingVarargMethod.class);
    checkNoMethodMatchFoundInClass(incorrectStaticMethodMatcher, ExampleClassUsingStaticMethod.class);
  }


  /**
   * Tests that we only match method used in specific parent context when that parent is present.
   */
  @Test
  public void testMatchMethodWithParent() {

    MethodMatcher correctVarargMethodMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithArgs")
        .isVarargs(true)
        .build();

    MethodMatcher correctNoArgMethodMatcherWithParent = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("baseMethod")
        .withFullyQualifiedParameters(new ArrayList<>())
        .withParentContext(correctVarargMethodMatcher)
        .build();

    checkMethodMatchFoundInClass(correctNoArgMethodMatcherWithParent, ExampleClassUsingMultipleMethods.class);
    checkNoMethodMatchFoundInClass(correctNoArgMethodMatcherWithParent, ExampleClassUsingNoArgBaseMethod.class); // this doesn't have the parent call around it.
  }


  /**
   * Tests that method matchers provide a useful toString.
   * Predicates are not by default logged usefully, so this tests that even where they are used, the output is still useful.
   */
  @Test
  public void testMethodMatcherPredicateLogging() {

    MethodMatcher matcherWithDefaultPredicateDescriptions = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType("com.Foo")
        .withMethodName("doFoo")
        .build();

    String customFQTypeDescription = "CUSTOM! package contains 'com'";
    String customNameDescription = "CUSTOM! starts with 'do'";

    MethodMatcher matcherWithDescribedPredicates = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(describedPredicate(customFQTypeDescription, t -> t.contains("com")))
        .withMethodName(describedPredicate(customNameDescription, n -> n.startsWith("do")))
        .withReturnType(describedPredicate("CUSTOM! returns Foobinator", r -> r.equals("Foobinator")))
        .build();

    assertEquals("MethodMatcher ["
        + "methodName=Optional[method name is [doFoo]], "
        + "fullyQualifiedDeclaringType=Optional[FQ type is [com.Foo]], "
        + "fullyQualifiedParameterNames=Optional.empty, "
        + "varArgs=Optional.empty, "
        + "parentContext=Optional.empty]",
        matcherWithDefaultPredicateDescriptions.toString());

    assertEquals("MethodMatcher ["
        + "methodName=Optional[" + customNameDescription + "], "
        + "fullyQualifiedDeclaringType=Optional[" + customFQTypeDescription + "], "
        + "fullyQualifiedParameterNames=Optional.empty, "
        + "varArgs=Optional.empty, "
        + "parentContext=Optional.empty]",
        matcherWithDescribedPredicates.toString());
  }


  /**
   * Only passes if the method matcher finds a match in the given class
   */
  private void checkMethodMatchFoundInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest) {
    checkMethodMatchInClass(methodMatcherToTest, clazzToTest, true);
  }

  /**
   * Only passes if the method matcher does not find a match in the given class.
   */
  private void checkNoMethodMatchFoundInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest){
    checkMethodMatchInClass(methodMatcherToTest, clazzToTest, false);
  }


  /**
   * Processes the class file and asserts whether the given MethodMatcher correctly finds a matching method or not.
   */
  private void checkMethodMatchInClass(MethodMatcher methodMatcherToTest, Class<?> clazzToTest, boolean shouldFindMatch) {
    File fileToTest = new File(TEST_EXAMPLES + "/" + clazzToTest.getName().replaceAll("\\.", "/") + ".java");
    String fileContentBefore = null;
    try {
      fileContentBefore = new String(Files.readAllBytes(fileToTest.toPath()));
    } catch (IOException e) {
      fail("IOException when reading file : " + e);
    }

    CompilationUnit compilationUnit = AstraUtils.readAsCompilationUnit(fileContentBefore, new String[] {TEST_SOURCE}, UseCase.DEFAULT_CLASSPATH_ENTRIES.toArray(new String[0]));

    final ClassVisitor visitor = new ClassVisitor();
    compilationUnit.accept(visitor);

    for (MethodInvocation methodInvocation : visitor.getMethodInvocations()) {
      if (methodMatcherToTest.matches(methodInvocation, compilationUnit)) {
        if (shouldFindMatch) {
          return; // we're looking for a match and found one.
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + methodInvocation.toString() + "]");
        }
      }
    }
    for (MethodDeclaration methodDeclaration : visitor.getMethodDeclarations()) {
      if (methodMatcherToTest.matches(methodDeclaration)) {
        if (shouldFindMatch) {
          return; // we're looking for a match and found one.
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + methodDeclaration.toString() + "]");
        }
      }
    }
    for (ClassInstanceCreation classInstanceCreation : visitor.getClassInstanceCreations()) {
      if (methodMatcherToTest.matches(classInstanceCreation)) {
        if (shouldFindMatch) {
          return; // we're looking for a match and found one.
        } else {
          fail("Should be no matches in class [" + clazzToTest.getName() + "] but found one at [" + classInstanceCreation.toString() + "]");
        }
      }
    }
    if (shouldFindMatch) {
      fail("Should have found a match for [" + methodMatcherToTest + "] within [" + fileContentBefore + "]");
    }
  }


  /**
   * Tests the static builder from fully qualified method signature
   */
  @Test
  public void testMethodMatcherBuiltFromFQSignature() {
    MethodMatcher noParams = MethodMatcher.buildMethodMatcherForFQSignature("com.Foo.doFoo()");
    MethodMatcher paramsNoSpaces = MethodMatcher.buildMethodMatcherForFQSignature("com.Foo.doFoo(int,com.Bar)");
    MethodMatcher paramsWithSpaces = MethodMatcher.buildMethodMatcherForFQSignature("com.Foo.doFoo(int, com.Bar)");

    assertEquals("FQ Type", "com.Foo", noParams.getFullyQualifiedDeclaringTypeExactName().get());
    assertEquals("FQ Type", "com.Foo", paramsNoSpaces.getFullyQualifiedDeclaringTypeExactName().get());
    assertEquals("FQ Type", "com.Foo", paramsWithSpaces.getFullyQualifiedDeclaringTypeExactName().get());

    assertEquals("Method name", "doFoo", noParams.getMethodNameExactName().get());
    assertEquals("Method name", "doFoo", paramsNoSpaces.getMethodNameExactName().get());
    assertEquals("Method name", "doFoo", paramsWithSpaces.getMethodNameExactName().get());

    assertEquals("Parameters", new ArrayList<>(), noParams.getFullyQualifiedParameterNames().get());
    assertEquals("Parameters", new ArrayList<>(Arrays.asList("int", "com.Bar")), paramsNoSpaces.getFullyQualifiedParameterNames().get());
    assertEquals("Parameters", new ArrayList<>(Arrays.asList("int", "com.Bar")), paramsWithSpaces.getFullyQualifiedParameterNames().get());
  }


  @Test
  public void testMethodMatcherGivenClassReferenceAsParamMatchesSameClassReference() {
    MethodMatcher classReferenceMatcher = MethodMatcher.builder()
        .withFullyQualifiedDeclaringType(ExampleUsedClass.class.getName())
        .withMethodName("methodWithClassArg")
        .withFullyQualifiedParameters(new ArrayList<>(Arrays.asList(Class.class.getName())))
        .build();

    checkMethodMatchFoundInClass(classReferenceMatcher, ExampleClassUsingMethodWithClassParameter.class);
  }


  @Test
  public void testMethodMatcherCustomPredicates() {
    MethodMatcher methodDeclarationMatcher = MethodMatcher.builder()
        .withMethodName("foo")
        .withCustomPredicate(
            DescribedPredicate.describedPredicate(
                "Method with no arguments",
                node -> node instanceof MethodDeclaration && ((MethodDeclaration)node).parameters().size() == 0))
        .build();

    MethodMatcher methodInvocationMatcher = MethodMatcher.builder()
        .withMethodName("foo")
        .withCustomPredicate(
            DescribedPredicate.describedPredicate(
                "Method with no arguments",
                node -> node instanceof MethodInvocation && ((MethodInvocation)node).arguments().size() == 0))
        .build();

    MethodMatcher classInstanceCreationMatcher = MethodMatcher.builder()
        .withMethodName("ExampleUsedClass")
        .withCustomPredicate(
            DescribedPredicate.describedPredicate(
                "Method with no arguments",
                node -> node instanceof ClassInstanceCreation && ((ClassInstanceCreation)node).arguments().size() == 0))
        .build();

    MethodMatcher classInstanceCreationMatcherNoMatch = MethodMatcher.builder()
        .withMethodName("ExampleUsedClass")
        .withCustomPredicate(
            DescribedPredicate.describedPredicate(
                "Method with no arguments",
                node -> node instanceof ClassInstanceCreation && ((ClassInstanceCreation)node).typeArguments().size() > 0))
        .build();

    checkMethodMatchFoundInClass(methodDeclarationMatcher, ExampleClassUsingMethodWithClassParameter.class);
    checkNoMethodMatchFoundInClass(methodInvocationMatcher, ExampleClassUsingMethodWithClassParameter.class);
    checkMethodMatchFoundInClass(classInstanceCreationMatcher, ExampleClassUsingMultipleMethods.class);
    checkNoMethodMatchFoundInClass(classInstanceCreationMatcherNoMatch, ExampleClassUsingMultipleMethods.class);
  }


  @Test
  public void testMethodMatcherForReturnTypeString() {
    MethodMatcher returnTypeStringMatcher = MethodMatcher.builder()
            .withReturnType(describedPredicate("Return type should be of type String", s  -> s.equals(String.class.getName())))
            .build();

    checkMethodMatchFoundInClass(returnTypeStringMatcher, ExampleClassUsingMethodWithReturnTypeString.class);
    checkNoMethodMatchFoundInClass(returnTypeStringMatcher, ExampleClassUsingMethodWithReturnTypeInt.class);
  }
}
