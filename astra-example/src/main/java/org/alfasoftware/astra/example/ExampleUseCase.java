package org.alfasoftware.astra.example;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.alfasoftware.astra.core.matchers.MethodMatcher;
import org.alfasoftware.astra.core.refactoring.UseCase;
import org.alfasoftware.astra.core.refactoring.operations.methods.MethodInvocationRefactor;
import org.alfasoftware.astra.core.utils.ASTOperation;

public class ExampleUseCase implements UseCase {

  @Override
  public Set<? extends ASTOperation> getOperations() {
    return new HashSet<>(
      Arrays.asList(
        MethodInvocationRefactor
          .from(
            MethodMatcher.builder()
              .withFullyQualifiedDeclaringType("org.alfasoftware.astra.example.target.FooBarInterface")
              .withMethodName("doFoo")
              .build())
          .to(
              new MethodInvocationRefactor.Changes().toNewMethodName("doBar")
          )
        )
    );
  }
}

