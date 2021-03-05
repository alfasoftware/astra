package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ExampleRule;
import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ExampleTestingModule;
import org.junit.Rule;
import org.junit.rules.MethodRule;

public class UnwrapInvocationAndChangeConstructorParentTypeExample {

  @Rule
  public final MethodRule testSetupRule = new ExampleRule(ExampleTestingModule.module(this),
    new Object() {
      @Override
      protected Object clone() throws CloneNotSupportedException {
        return super.clone();
      }
  });
}