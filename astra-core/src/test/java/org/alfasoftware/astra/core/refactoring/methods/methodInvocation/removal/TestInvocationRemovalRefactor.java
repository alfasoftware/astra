package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.AbstractRefactorTest;
import org.alfasoftware.astra.core.refactoring.methods.constructortobuilder.ConstructorToBuilderInnerClassBuilderExampleMatcher1;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ExampleRule;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ExampleTestingModule;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ReplacementRule;
import org.alfasoftware.astra.core.refactoring.operations.javapattern.JavaPatternASTOperation;
import org.alfasoftware.astra.core.refactoring.operations.methods.ChainedMethodInvocationRemovalRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.UnwrapAndRemoveMethodInvocationRefactor;
import org.alfasoftware.astra.core.refactoring.operations.methods.UnwrapAndRemoveMethodInvocationRefactor.Changes;
import org.alfasoftware.astra.exampleTypes.ExampleWithFluentBuilder;
import org.junit.Test;

public class TestInvocationRemovalRefactor extends AbstractRefactorTest {

  @Test
  public void testInvocationChainedRemoval() {
    assertRefactor(
      RemoveChainedInvocationExample.class,
      new HashSet<>(Arrays.asList(
        new ChainedMethodInvocationRemovalRefactor(
          MethodMatcher.builder()
            .withMethodName("withB")
            .withFullyQualifiedDeclaringType(ExampleWithFluentBuilder.Builder.class.getName())
            .build()),
        new ChainedMethodInvocationRemovalRefactor(
          MethodMatcher.builder()
            .withMethodName("withBs")
            .withFullyQualifiedDeclaringType(ExampleWithFluentBuilder.Builder.class.getName())
            .build())
      ))
    );
  }

  @Test
  public void testInvocationChainedRemovalMatcher() throws IOException {
    assertRefactor(
        RemoveChainedInvocationExample.class,
        Collections.singleton(
            new JavaPatternASTOperation(
                new File(TEST_EXAMPLES + "/" + RemoveChainedInvocationExampleMatcher.class.getName().replaceAll("\\.", "/") + ".java"),
                new String[]{TEST_SOURCE}
            )
        )
    );
  }


  /**
   * Unwraps an invocation by using the parameters in the (constructor) parent call that included it.
   * Also changes the type of the parent object being constructed.
   */
  @Test
  public void testChangeConstructorParentAndRemoveStaticInvocation() {
    assertRefactor(UnwrapInvocationAndChangeConstructorParentTypeExample.class,
        new HashSet<>(Arrays.asList(
            UnwrapAndRemoveMethodInvocationRefactor
              .from(
                MethodMatcher.builder()
                .withMethodName("module")
                .withFullyQualifiedDeclaringType(ExampleTestingModule.class.getName())
                .withParentContext(
                  MethodMatcher.builder()
                  .withMethodName(ExampleRule.class.getSimpleName())
                  .withFullyQualifiedDeclaringType(ExampleRule.class.getName())
                  .build())
                .build()
                )
              .to(new Changes().toNewParentType(ReplacementRule.class.getName())))
        ));
  }


  /**
   * Unwraps an invocation by using the parameters in the (method) parent call that included it.
   * Also changes the type and method name of the parent call
   */
  @Test
  public void testChangeMethodParentAndRemoveStaticInvocation() {
    assertRefactor(UnwrapInvocationAndChangeMethodParentTypeExample.class,
        new HashSet<>(Arrays.asList(
            UnwrapAndRemoveMethodInvocationRefactor
              .from(
                MethodMatcher.builder().
                withMethodName("module").
                withFullyQualifiedDeclaringType(ExampleTestingModule.class.getName())
                .withParentContext(
                  MethodMatcher.builder().
                  withFullyQualifiedDeclaringType(ExampleRule.class.getName())
                  .withMethodName("ruleFor")
                  .build())
                .build()
                )
              .to(new Changes()
                .toNewParentType(ReplacementRule.class.getName())
                .toNewParentMethodName("replacementRuleFor")
                .atParameterPosition(1)))
        ));
  }
}

