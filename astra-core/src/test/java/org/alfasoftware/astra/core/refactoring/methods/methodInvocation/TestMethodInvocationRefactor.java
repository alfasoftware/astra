package org.alfasoftware.astra.core.refactoring.methods.methodInvocation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.edge.UnknownTypeInLambdaExample;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform.InvocationTransformExample;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform.InvocationTransformExampleMatcher;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.transform.ReturnsObject;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.alfasoftware.astra.core.refactoring.operations.methods.ChainedMethodInvocationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor.Changes;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor.Position;
import org.alfasoftware.astra.core.refactoring.operations.types.TypeReferenceRefactor;
import org.alfasoftware.astra.exampleTypes.A;
import org.alfasoftware.astra.exampleTypes.B;
import org.alfasoftware.astra.exampleTypes.BaseFooable;
import org.alfasoftware.astra.exampleTypes.C;
import org.alfasoftware.astra.exampleTypes.Fooable;
import org.eclipse.jdt.core.dom.CastExpression;
import org.junit.Ignore;
import org.junit.Test;


public class TestMethodInvocationRefactor extends AbstractRefactorTest {

  /**
   * Changes an invocation of a method from a specific class,
   * with a specific name, to another method on the same class.
   * Method invocations with the same name, but declared on a different class to the one specified,
   * should not be changed.
   */
  @Test
  public void testInvocationChangeOutsideClassFullyQualified() {
	  assertRefactor(InvocationChangeOutsideClassQualifiedExample.class,
			  new HashSet<>(Arrays.asList(
			      MethodInvocationRefactor
			        .from(MethodMatcher.builder()
			          .withMethodName("notify")
			          .withFullyQualifiedDeclaringType(A.class.getName())
			          .build())
		          .to(new Changes()
		            .toNewMethodName("getClass")))));
  }


  /**
   * Uses the JavaPattern framework to change an invocation of a method from a specific class,
   * with a specific name, to another method on the same class.
   */
  @Test
  public void testInvocationChangeOutsideClassFullyQualifiedUsingMatcher() throws IOException {
    assertRefactor(InvocationChangeOutsideClassQualifiedExample.class,
        new HashSet<>(
            Arrays.asList(
              new JavaPatternASTOperation(
		  new File(TEST_EXAMPLES + "/" + InvocationChangeOutsideClassQualifiedExampleMatcher.class.getName().replaceAll("\\.", "/") + ".java"),
                  new String[]{TEST_SOURCE}
              )
            )
        )
    );
  }
  
  
  /**
   * Changes an invocation of a method inherited from a class with a specific name, to another method.
   * Method invocations with the same name, but declared on a different class to the one specified,
   * should not be changed.
   */
  @Test
  public void testInvocationChangeInheritedFromSuperclass() {
    assertRefactor(InvocationChangeInheritedFromSuperclassExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("notify")
                .withFullyQualifiedDeclaringType(Object.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("getClass")))));
  }

  /*
   * Uses the JavaPattern framework to change an invocation of a method inherited from a class with a specific name, to another method.
   */
  @Test
  public void testInvocationChangeInheritedFromSuperclassMatcher() throws IOException {
    assertRefactor(InvocationChangeInheritedFromSuperclassExample.class,
        new HashSet<>(
            Collections.singletonList(
                new JavaPatternASTOperation(
		  new File(TEST_EXAMPLES + "/" + InvocationChangeInheritedFromSuperclassMatcher.class.getName().replaceAll("\\.", "/") + ".java"))
            )
        )
    );
  }

  
  /**
   * Changes an invocation of a method inherited from an interface with a specific name, to another method.
   * Method invocations with the same name, but declared on a different class to the one specified,
   * should not be changed.
   */
  @Test
  public void testInvocationChangeInheritedFromInterface() {
    assertRefactor(InvocationChangeInheritedFromInterfaceExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("doFoo")
                .withFullyQualifiedDeclaringType(Fooable.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("getClass")))));
  }
  
  
  /**
   * Changes an invocation of a method inherited from an interface which extends another interface, to another method.
   * Method invocations with the same name, but declared on a different class to the one specified,
   * should not be changed.
   */
  @Test
  public void testInvocationChangeInheritedFromExtensionOfInterface() {
    assertRefactor(InvocationChangeInheritedFromInterfaceExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("doFoo")
                .withFullyQualifiedDeclaringType(BaseFooable.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("getClass")))));
  }
  

  /**
   * Changes a static method invocation target.
   * The method target is the type that the method is executed against.
   */
  @Test
  public void testInvocationChangeStatic() {
    assertRefactor(InvocationChangeStaticExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("staticOne")
                .withFullyQualifiedDeclaringType(A.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("staticTwo")
                .toNewType(B.class.getName())))));
  }


  /**
   * Changes a static method invocation target.
   * The method target is the type that the method is executed against.
   * Tests where multiple invocations are changed.
   */
  @Test
  public void testInvocationChangeStaticMultiple() {
    assertRefactor(InvocationChangeStaticMultipleExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("staticOne")
                .withFullyQualifiedDeclaringType(A.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("staticTwo")
                .toNewType(B.class.getName())))));
  }


  /**
   * Changes a static method invocation target.
   * The method target is the type that the method is executed against.
   * Only methods from that target should be changed.
   */
  @Test
  public void testInvocationChangeStaticSameName() {
    assertRefactor(InvocationChangeStaticSameNameExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("staticOne")
                .withFullyQualifiedDeclaringType(A.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("staticTwo")
                .toNewType(C.class.getName())))));
  }


  /**
   * Changes a static method invocation target.
   * The method target is the type that the method is executed against.
   * Only methods from that target (as defined by the package) should be changed.
   */
  @Test
  public void testInvocationChangeStaticSameNameDifferentPackage() {
    assertRefactor(InvocationChangeStaticDifferentPackageExample.class,
        new HashSet<>(Arrays.asList(
            MethodInvocationRefactor
              .from(MethodMatcher.builder()
                .withMethodName("staticOne")
                .withFullyQualifiedDeclaringType(org.alfasoftware.astra.exampleTypes.a.A.class.getName())
                .build())
              .to(new Changes()
                .toNewMethodName("staticTwo")))));
  }


  @Test
  public void testInvocationChangeWithAnotherUseOfImport() {
    assertRefactor(InvocationChangeWithAnotherUseOfImport.class,
      new HashSet<>(Arrays.asList(
          MethodInvocationRefactor
            .from(MethodMatcher.builder()
              .withMethodName("staticOne")
              .withFullyQualifiedDeclaringType(A.class.getName())
              .build())
            .to(new Changes()
              .toNewType(B.class.getName())))));
  }

  @Test
  public void testInvocationChangeWithNewParameterStringInLastPosition() {
    assertRefactor(InvocationChangeNewParameterStringLast.class,
      new HashSet<>(Arrays.asList(
          MethodInvocationRefactor
            .from(MethodMatcher.builder()
              .withMethodName("overloaded")
              .withFullyQualifiedDeclaringType(A.class.getName())
              .build())
            .to(new Changes()
              .withNewParameter("\"NEW\"", Position.LAST)))));
  }

  @Test
  public void testInvocationChained() {
    assertRefactor(InvocationChainedExample.class,
      new HashSet<>(Arrays.asList(
        new ChainedMethodInvocationRefactor(
          Arrays.asList(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("first")
              .build(),
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("second")
              .build()),
          Arrays.asList("third")),
        TypeReferenceRefactor.builder()
          .fromType(A.class.getName())
          .toType(C.class.getName())
          .build()
      ))
    );
  }
  
  
  /**
   * ChainedMethodInvocationRefactor should be able to handle chains of arbitrary length.
   * This means we should be able to match on arbitrarily large chains of method calls,
   * and replace them with chains of method calls.
   * 
   * Both the matching and replacement of method calls currently stops after two.
   */
  @Ignore("Illustrates issue https://github.com/alfasoftware/astra/issues/36")
  @Test
  public void testInvocationChainedWithLargeMatchingAndReplacementChains() {
    assertRefactor(InvocationChainedLargeChainsExample.class,
      new HashSet<>(Arrays.asList(
        new ChainedMethodInvocationRefactor(
          Arrays.asList(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("getA")
              .build(),
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("getB")
              .build(),
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(B.class.getName())
              .withMethodName("getC")
              .build(),
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(C.class.getName())
              .withMethodName("getD")
              .build()),
          Arrays.asList("getD", "getC", "getB", "getA")),
        new ChainedMethodInvocationRefactor(
            Arrays.asList(
              MethodMatcher.builder()
                .withFullyQualifiedDeclaringType(A.class.getName())
                .withMethodName("getC")
                .build(),
              MethodMatcher.builder()
                .withFullyQualifiedDeclaringType(C.class.getName())
                .withMethodName("getB")
                .build(),
              MethodMatcher.builder()
                .withFullyQualifiedDeclaringType(B.class.getName())
                .withMethodName("getA")
                .build()),
            Arrays.asList("getA", "getB", "getC"))
      ))
    );
  }
  

  @Test
  public void testInvocationChainedWrapped() {
    assertRefactor(InvocationChainedWrappedExample.class,
      new HashSet<>(Arrays.asList(
        new ChainedMethodInvocationRefactor(
          Arrays.asList(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("first")
              .build(),
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType(A.class.getName())
              .withMethodName("second")
              .build()),
          Arrays.asList("third")),
        TypeReferenceRefactor.builder().fromType(A.class.getName()).toType(C.class.getName()).build()
      ))
    );
  }


  /**
   * Tests using .withInvocationTransform to introduce an arbitrary transformation.
   * In this case, an unnecessary cast is removed.
   */
  @Test
  public void testInvocationChangeWithTransform() {
    assertRefactor(InvocationTransformExample.class,
      new HashSet<>(Arrays.asList(
        MethodInvocationRefactor
          .from(MethodMatcher.builder()
            .withFullyQualifiedDeclaringType(ReturnsObject.class.getName())
            .withMethodName("get")
            .build())
          .to(new Changes()
            .withInvocationTransform((compilationUnit, methodInvocation, rewriter) -> {
              if (methodInvocation.getParent() instanceof CastExpression) {
                CastExpression castExpression = (CastExpression) methodInvocation.getParent();
                // Replaces the whole cast expression, eg "(ReturnsObject)instance.get()"
                // with just the variable expression, eg "instance"
                rewriter.replace(castExpression, methodInvocation.getExpression(), null);
              }
            })
          )
        )
      )
    );
  }


  /**
   * Tests using .withInvocationTransform to introduce an arbitrary transformation.
   * In this case, an unnecessary cast is removed.
   */
  @Test
  public void testInvocationChangeWithTransformMatcher() throws IOException {
    assertRefactor(InvocationTransformExample.class,
        new HashSet<>(Collections.singletonList(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + InvocationTransformExampleMatcher.class.getName().replaceAll("\\.", "/") + ".java"),
                new String[]{TEST_SOURCE}))
        )
    );
  }


  @Test
  @Ignore("In this case, we find that we can't resolve bindings inside a lambda, where the lambda has any prior unknown types." + 
    " Here, that means that even though we have the class files for a method invocation, because we're missing the class" + 
    " files for a type referenced in the lambda, we cannot resolve the method invocation." + 
    " We need this information for type inference - https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html" + 
    " Because we don't have that type information, we don't know the return type, which means we are also missing" + 
    " the type which could be used as a type parameter, or an argument to a later function - so we can't determine which" + 
    " of a number of overloaded methods are being called, or if we have met the specific type criteria for a method." + 
    " The best we could do is match a method which uses 'Object'. In a later release, it's possible that Eclipse JDT Core" + 
    " may attempt a 'best guess' - but this sounds like it's not planned, and is considered a difficult problem to solve." + 
    " In this test, the unknown type is 'ASTNode'. If we provided the classpath for org.eclipse.jdt.core, the ASTNode type" + 
    " would be resolved, the rest of the lambda would be evaluated, the method identified and refactored, and the test would pass." + 
    " The 'proper' solution for this tool is to run with all the class files provided to the AST - but this makes the tool" + 
    " take significantly longer to run, due to the extra time taken to generate each file's AST." + 
    " If 100% accuracy is required, provide all the class files and accept the slow run.")
  public void testInvocationInLambdaWithUnknownType() {
    assertRefactor(
      UnknownTypeInLambdaExample.class,
      new HashSet<>(Arrays.asList(
        MethodInvocationRefactor
          .from(
            MethodMatcher.builder()
              .withMethodName("equals")
              .withFullyQualifiedDeclaringType(Objects.class.getName())
              .build())
          .to(
            new Changes()
              .toNewType(Objects.class.getName())
              .toNewMethodName("deepEquals")
              .withStaticImportInlined()))));
  }
}

