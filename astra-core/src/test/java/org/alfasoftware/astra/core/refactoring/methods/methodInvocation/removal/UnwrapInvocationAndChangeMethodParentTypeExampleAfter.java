package org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal;

import org.alfasoftware.astra.core.refactoring.methods.methodInvocation.removal.sub.ReplacementRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;

public class UnwrapInvocationAndChangeMethodParentTypeExampleAfter {

  @Rule
  public final MethodRule testSetupRule = ReplacementRule.replacementRuleFor("Some String", this, new Object() {
      @Override
      protected Object clone() throws CloneNotSupportedException {
        return super.clone();
      }
  });

}